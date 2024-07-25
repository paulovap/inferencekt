//
// Created by paulo on 04/06/2024.
//

#ifndef PINEAI_LLAMAKT_H
#define PINEAI_LLAMAKT_H

#ifdef __cplusplus
extern "C" {
#endif

#include "llama.h"

    struct llama_model;
    struct llama_context;
    struct llama_batch;

    void init_llama_backend();
    struct llama_model *platform_load_model(const char* path);
    void platform_unload_model(struct llama_model *);
    struct llama_context *platform_new_context(struct llama_model* model);
    void platform_delete_context(struct llama_context *context);
    struct llama_batch *platform_new_batch(int nTokens, int embd, int n_seq_max);
    void platform_delete_batch(struct llama_batch *batch);
    const char* platform_completion_loop(struct llama_context *context, struct llama_batch *batch, int n_len, int n_cur);
    int platform_completion_init(struct llama_context * context, struct llama_batch * batch, const char* prompt, int n_len);
    void platform_kv_cache_clear(struct llama_context * context);
    double platform_tokens_per_second(struct llama_context* context);

#ifdef __cplusplus
}
#endif

#endif //PINEAI_LLAMAKT_H
