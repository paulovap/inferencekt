//#include <cstdio>
#include "llama.h"
#include "common/common.h"

extern "C" {


const char *hi() {
    return llama_print_system_info();
}

}
