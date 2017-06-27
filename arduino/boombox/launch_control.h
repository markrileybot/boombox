#ifndef LAUNCH_CONTROL_H
#define LAUNCH_CONTROL_H

#include "proto.h"

#ifdef __cplusplus
extern "C" {
#endif

bool launch_control_init();

void launch_control_set_sequence(launch_t *cmd);

void launch_control_reset();

bool launch_control_ready();

void launch_control_fire();

#ifdef __cplusplus
}
#endif
#endif

