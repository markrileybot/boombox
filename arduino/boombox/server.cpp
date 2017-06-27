#include <Arduino.h>
#include "server.h"
#include "proto.h"
#include "launch_control.h"

#define MSG_MAX 128

const byte *MAGIC_FOOTER = (byte[]) {BOOMBOX_MAGIC0, BOOMBOX_MAGIC1, BOOMBOX_MAGIC2, BOOMBOX_MAGIC3};

bool buffer_full = false;
uint32_t message_len = 0;
byte message[MSG_MAX];

bool server_init() {
  message_len = 0;
  memset(message, 0, MSG_MAX);
  return true;
}

bool server_reset() {
  message_len = 0;
  memset(message, 0, MSG_MAX);
  return true;
}

bool server_receive(const byte *chunk, unsigned int chunk_len, 
  const byte** resp, unsigned int *resp_len) {
  const byte *foot;
  
  // append the data to the message buffer
  if (chunk_len > 0) {
    if (message_len + chunk_len > MSG_MAX) {
       // fail!
       memset(message, 0, MSG_MAX);
       message[OFFSET_TYPE] = (byte) BOOMBOX_MESSAGE_TYPE_ERROR;
       buffer_full = true;
    } else {
      memcpy(&message[message_len], chunk, chunk_len);
      message_len += chunk_len;
    
      // Did we see EOM?
      foot = &message[message_len - MAGIC_FOOTER_LEN];
      if (message_len >= MAGIC_FOOTER_LEN && memcmp(foot, MAGIC_FOOTER, MAGIC_FOOTER_LEN) == 0) {
        message_len -= MAGIC_FOOTER_LEN;
        buffer_full = true;
      }
    }
  }

  if (buffer_full) {
    switch ((int)message[OFFSET_TYPE]) {
      case BOOMBOX_MESSAGE_TYPE_LAUNCH:
        launch_control_set_sequence((launch_t*) &message[OFFSET_MSG]);
        break;
      case BOOMBOX_MESSAGE_TYPE_PING:
        break;
      case BOOMBOX_MESSAGE_TYPE_RESET:
//        launch_control_reset();
        break;
      default:
        message[OFFSET_TYPE] = (byte) BOOMBOX_MESSAGE_TYPE_ERROR;
        break;
    }

    *resp = message;
    *resp_len = OFFSET_MSG;

    memset(&message[message_len], 0, 4);
    message_len = 0;
    buffer_full = false;
    return true;
  }

  return false;
}


