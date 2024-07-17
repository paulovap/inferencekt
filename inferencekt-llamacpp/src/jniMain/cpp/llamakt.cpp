#include <string>
#include <cstdio>
#include "llama.h"
#include "common/common.h"
#include "llamakt.h"

#ifdef __ANDROID__
#include <android/log.h>


#define TAG "llamakt.cpp"
#define LOGi(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGe(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#else
#define LOGi(...) printf(__VA_ARGS__)
#define LOGe(...) printf(__VA_ARGS__)
#endif // __ANDROID__

bool is_valid_utf8(const char * string) {
    if (!string) {
        return true;
    }

    const unsigned char * bytes = (const unsigned char *)string;
    int num;

    while (*bytes != 0x00) {
        if ((*bytes & 0x80) == 0x00) {
            // U+0000 to U+007F
            num = 1;
        } else if ((*bytes & 0xE0) == 0xC0) {
            // U+0080 to U+07FF
            num = 2;
        } else if ((*bytes & 0xF0) == 0xE0) {
            // U+0800 to U+FFFF
            num = 3;
        } else if ((*bytes & 0xF8) == 0xF0) {
            // U+10000 to U+10FFFF
            num = 4;
        } else {
            return false;
        }

        bytes += 1;
        for (int i = 1; i < num; ++i) {
            if ((*bytes & 0xC0) != 0x80) {
                return false;
            }
            bytes += 1;
        }
    }

    return true;
}

void init_llama_backend() {
    llama_backend_init();
}

llama_model *platform_load_model(const char* path) {
    llama_model_params model_params = llama_model_default_params();
    auto model = llama_load_model_from_file(path, model_params);

    return model;
}

llama_context *platform_new_context(llama_model* model) {

    if (!model) {
        LOGe("new_context(): model cannot be null");
        return 0;
    }

    int n_threads = std::max(1, 4);
    LOGi("Using %d threads", n_threads);

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.seed  = 1234;
    ctx_params.n_ctx = 512;
    ctx_params.n_threads       = n_threads;
    ctx_params.n_threads_batch = n_threads;
    //ctx_params.flash_attn = true;

    llama_context * context = llama_new_context_with_model(model, ctx_params);

    if (!context) {
        LOGe("llama_new_context_with_model() returned null)");
        return nullptr;
    }

    return context;
}

llama_batch * platform_new_batch(int n_tokens, int embd, int n_seq_max){
    auto batch = new llama_batch {
            0,
            nullptr,
            nullptr,
            nullptr,
            nullptr,
            nullptr,
            nullptr,
            0,
            0,
            0,
    };

    if (embd) {
        batch->embd = (float *) malloc(sizeof(float) * n_tokens * embd);
    } else {
        batch->token = (llama_token *) malloc(sizeof(llama_token) * n_tokens);
    }

    batch->pos      = (llama_pos *)     malloc(sizeof(llama_pos)      * n_tokens);
    batch->n_seq_id = (int32_t *)       malloc(sizeof(int32_t)        * n_tokens);
    batch->seq_id   = (llama_seq_id **) malloc(sizeof(llama_seq_id *) * n_tokens);
    for (int i = 0; i < n_tokens; ++i) {
        batch->seq_id[i] = (llama_seq_id *) malloc(sizeof(llama_seq_id) * n_seq_max);
    }
    batch->logits   = (int8_t *)        malloc(sizeof(int8_t)         * n_tokens);

    return batch;
}

int platform_completion_init(struct llama_context* context, struct llama_batch *batch, const char* text, int n_len) {
    const auto tokens_list = llama_tokenize(context, text, 1);

    auto n_ctx = llama_n_ctx(context);
    auto n_kv_req = tokens_list.size() + (n_len - tokens_list.size());

    LOGi("n_len = %d, n_ctx = %d, n_kv_req = %d", n_len, n_ctx, n_kv_req);

    if (n_kv_req > n_ctx) {
        LOGe("error: n_kv_req > n_ctx, the required KV cache size is not big enough");
    }

    for (auto id : tokens_list) {
        LOGi("%s", llama_token_to_piece(context, id).c_str());
    }

    llama_batch_clear(*batch);

// evaluate the initial prompt
    for (auto i = 0; i < tokens_list.size(); i++) {
        llama_batch_add(*batch, tokens_list[i], i, { 0 }, false);
    }

// llama_decode will output logits only for the last token of the prompt
    batch->logits[batch->n_tokens - 1] = true;

    if (llama_decode(context, *batch) != 0) {
        LOGe("llama_decode() failed");
    }

    return batch->n_tokens;
}

const char* platform_completion_loop(struct llama_context* context, struct llama_batch *batch, int n_len, int n_cur) {
    const auto model = llama_get_model(context);
    auto n_vocab = llama_n_vocab(model);
    auto logits = llama_get_logits_ith(context, batch->n_tokens - 1);

    std::vector<llama_token_data> candidates;
    candidates.reserve(n_vocab);

    for (llama_token token_id = 0; token_id < n_vocab; token_id++) {
        candidates.emplace_back(llama_token_data{ token_id, logits[token_id], 0.0f });
    }

    llama_token_data_array candidates_p = { candidates.data(), candidates.size(), false };

// sample the most likely token
    const auto new_token_id = llama_sample_token_greedy(context, &candidates_p);

    //TODO: find a way to increment n_cur and keep the state somewhere.
    if (llama_token_is_eog(model, new_token_id) || n_cur == n_len) {
        const auto timings = llama_get_timings(context);
        LOGe("\n");
        LOGe("%s:        load time = %10.2f ms\n", __func__, timings.t_load_ms);
        LOGe("%s:      sample time = %10.2f ms / %5d runs   (%8.2f ms per token, %8.2f tokens per second)\n",
             __func__, timings.t_sample_ms, timings.n_sample, timings.t_sample_ms / timings.n_sample, 1e3 / timings.t_sample_ms * timings.n_sample);
        LOGe("%s: prompt eval time = %10.2f ms / %5d tokens (%8.2f ms per token, %8.2f tokens per second)\n",
             __func__, timings.t_p_eval_ms, timings.n_p_eval, timings.t_p_eval_ms / timings.n_p_eval, 1e3 / timings.t_p_eval_ms * timings.n_p_eval);
        LOGe("%s:        eval time = %10.2f ms / %5d runs   (%8.2f ms per token, %8.2f tokens per second)\n",
             __func__, timings.t_eval_ms, timings.n_eval, timings.t_eval_ms / timings.n_eval, 1e3 / timings.t_eval_ms * timings.n_eval);
        LOGe("%s:       total time = %10.2f ms / %5d tokens\n", __func__, (timings.t_end_ms - timings.t_start_ms), (timings.n_p_eval + timings.n_eval));
        return nullptr;
    }

    auto new_token_chars = llama_token_to_piece(context, new_token_id);

    if (is_valid_utf8(new_token_chars.c_str())) {
        LOGi("id: %d, token: `%s`\n", new_token_id, new_token_chars.c_str());
    } else {
        LOGe("platform_completion_loop() not valid UTF*");
    }

    llama_batch_clear(*batch);
    llama_batch_add(*batch, new_token_id, n_cur, { 0 }, true);

    if (llama_decode(context, *batch) != 0) {
        LOGe("llama_decode() returned null");
    }

    //#todo is it possible to avoid this copy?
    return strdup(new_token_chars.c_str());
}

void platform_kv_cache_clear(struct llama_context *context) {
    llama_kv_cache_clear(reinterpret_cast<llama_context *>(context));
}

#ifdef JNI

#include <jni.h>

jclass la_int_var;
jmethodID la_int_var_value;
jmethodID la_int_var_inc;


extern "C"
JNIEXPORT jstring JNICALL
Java_org_pinelang_inferencekt_llamacpp_Llamacpp_1jniKt_nativeSystemInfo(JNIEnv *env, jobject) {
    return env->NewStringUTF(llama_print_system_info());
}
extern "C"
JNIEXPORT void JNICALL
Java_org_pinelang_inferencekt_llamacpp_Llamacpp_1jniKt_initLlamaBackend(JNIEnv *env, jclass clazz) {
    init_llama_backend();
}
extern "C"
JNIEXPORT jlong JNICALL
Java_org_pinelang_inferencekt_llamacpp_Llamacpp_1jniKt_nativeLoadModel(JNIEnv *env, jclass clazz, jstring path) {
    auto path_to_model = env->GetStringUTFChars(path, 0);
    auto model = platform_load_model(path_to_model);
    env->ReleaseStringUTFChars(path, path_to_model);

    if (!model) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), "load_model() failed");
        return 0;
    }
    return reinterpret_cast<long>(model);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_org_pinelang_inferencekt_llamacpp_Llamacpp_1jniKt_nativeNewContext(JNIEnv *env, jobject, jlong jmodel) {
    auto model = reinterpret_cast<llama_model *>(jmodel);

    if (!model) {
        LOGe("new_context(): model cannot be null");
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "Model cannot be null");
        return 0;
    }

    return reinterpret_cast<long>(platform_new_context(model));
}
extern "C"
JNIEXPORT jlong JNICALL
Java_org_pinelang_inferencekt_llamacpp_Llamacpp_1jniKt_nativeNewBatch(JNIEnv *env, jobject, jint n_tokens, jint embd,
                                              jint n_seq_max) {
    return reinterpret_cast<jlong>(platform_new_batch(n_tokens, embd, n_seq_max));
}
extern "C"
JNIEXPORT jint JNICALL
Java_org_pinelang_inferencekt_llamacpp_Llamacpp_1jniKt_nativeCompletionInit(JNIEnv *env, jobject, jlong context_pointer,
                                                                   jlong batch_pointer, jstring jtext, jint n_len) {
    const auto text = env->GetStringUTFChars(jtext, 0);
    const auto context = reinterpret_cast<llama_context *>(context_pointer);
    const auto batch = reinterpret_cast<llama_batch *>(batch_pointer);

    return platform_completion_init(context, batch, text, n_len);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_org_pinelang_inferencekt_llamacpp_Llamacpp_1jniKt_nativeCompletionLoop(
        JNIEnv *env,
        jclass,
        jlong context_pointer,
        jlong batch_pointer,
        jint n_len,
        jobject intvar_ncur) {
    const auto context = reinterpret_cast<llama_context *>(context_pointer);
    const auto batch = reinterpret_cast<llama_batch *>(batch_pointer);
    const auto model = llama_get_model(context);

    if (!la_int_var) la_int_var = env->GetObjectClass(intvar_ncur);
    if (!la_int_var_value) la_int_var_value = env->GetMethodID(la_int_var, "getValue", "()I");
    if (!la_int_var_inc) la_int_var_inc = env->GetMethodID(la_int_var, "inc", "()V");
    const auto n_cur = env->CallIntMethod(intvar_ncur, la_int_var_value);
    auto result = platform_completion_loop(context, batch, n_len, n_cur);
    return env->NewStringUTF(result);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_pinelang_inferencekt_llamacpp_Llamacpp_1jniKt_nativeKvCacheClear(JNIEnv *env, jclass clazz, jlong context) {
    llama_kv_cache_clear(reinterpret_cast<llama_context *>(context));
}

#endif // JNI


