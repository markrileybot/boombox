
#ifndef SERVER_H
#define SERVER_H

#include <stdbool.h>
#include <Arduino.h>
#include "common.h"

#ifdef __cplusplus
extern "C" {
#endif

bool server_init(io_handler handler);

bool server_reset();

bool server_process(const byte* chunk, unsigned int len);

#ifdef __cplusplus
}
#endif
#endif

