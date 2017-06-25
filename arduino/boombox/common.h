
#ifndef COMMON_H
#define COMMON_H

#ifdef __cplusplus
extern "C" {
#endif

typedef void (*io_handler)(const byte *msg, unsigned int len);
typedef void (*command_processor)(void *command);
typedef void (*reset_handler)();

#ifdef __cplusplus
}
#endif

#endif
