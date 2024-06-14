//
// Created by paulo on 04/06/2024.
//

#ifndef PINEAI_LLAMAKT_NOJNI_H
#define PINEAI_LLAMAKT_NOJNI_H

#include "llama.h"


void init_llama_backend();
long platform_load_model(const char* path);
long platform_new_context(long model);
long platform_new_batch(int nTokens, int embd, int n_seq_max);
const char* platform_completion_loop(long context, long batch, int n_len, int n_cur);
int platform_completion_init(long context, long batch, const char* prompt, int n_len);
void platform_kv_cache_clear(long context);
#endif //PINEAI_LLAMAKT_NOJNI_H
