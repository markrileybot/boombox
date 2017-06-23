
#include <Arduino.h>
#include "server.h"
#include <thrift_nano.h>
#include "boombox_boombox_types.h"

char *magic_footer = (char[]) {BOOMBOX_MAGIC0, BOOMBOX_MAGIC1, BOOMBOX_MAGIC2, BOOMBOX_MAGIC3};

io_handler response_handler;

struct tn_transport_memory_t mem_transport;
tn_transport_t *transport;
tn_protocol_t *protocol;
tn_buffer_t *buf;

boombox_message_t                           *message_header;
boombox_launch_t                            *launch_command;
boombox_launch_response_t                   *launch_command_response;
boombox_get_launch_tube_state_t             *state_command;
boombox_get_launch_tube_state_response_t    *state_command_response;
boombox_error_t                             *error_response;

bool server_init(io_handler cb) {
  tn_error_t error = T_ERR_OK;
  response_handler = cb;
  
  tn_init();
  
  if ((transport = tn_transport_memory_init(&mem_transport, 255, &error)) == NULL) {
    return false;
  }
  if ((protocol = tn_protocol_compact_create(&error)) == NULL) {
    return false;
  }
  if ((message_header = boombox_message_create(&error)) == NULL) {
    return false;
  }
  if ((launch_command = boombox_launch_create(&error)) == NULL) {
    return false;
  }
  if ((launch_command_response = boombox_launch_response_create(&error)) == NULL) {
    return false;
  }  
  if ((state_command = boombox_get_launch_tube_state_create(&error)) == NULL) {
    return false;
  }
  if ((state_command_response = boombox_get_launch_tube_state_response_create(&error)) == NULL) {
    return false;
  }
  if ((error_response = boombox_error_create(&error)) == NULL) {
    return false;
  }

  buf = mem_transport.buf;
  return true;
}

bool server_reset() {
  tn_object_reset(transport);
  tn_object_reset(protocol);
  tn_object_reset(message_header);
  tn_object_reset(launch_command);
  tn_object_reset(launch_command_response);
  tn_object_reset(state_command);
  tn_object_reset(state_command_response);
  tn_object_reset(error_response);
  return true;
}

void * server_process_launch() {
  boombox_launch_tube_t *v;
  if (launch_command->tubes != NULL) {
    size_t size = launch_command->tubes->elem_count;
    for(size_t i = 0; i < size; i++) {
      v = *(boombox_launch_tube_t**)tn_list_get(launch_command->tubes, i);
      Serial.print("FIRE: ");
      Serial.println(v->position);
      digitalWrite(v->position, HIGH);
    }
    delay(100);
    for(size_t i = 0; i < size; i++) {
      v = *(boombox_launch_tube_t**)tn_list_get(launch_command->tubes, i);
      digitalWrite(v->position, LOW);
    }
    return launch_command_response;
  }
  return NULL;
}

void * server_process_get_state() {
  return state_command_response;
}

bool server_process(const byte *val, unsigned int msgLen) {
  tn_error_t error = T_ERR_OK;
  void *response = NULL;

  // append the data to the message buffer
  if (msgLen > 0) {
    tn_transport_write(transport, (void*)val, msgLen, &error);
  }

  // Did we see EOM?
  size_t len = buf->len;
  void *foot = buf->buf + (len - 4);
  if (len >= 4 && memcmp(foot, magic_footer, 4) == 0) {
    tn_transport_memory_rewind(&mem_transport);
    Serial.print("0 POS=");
    Serial.println(len);
    tn_object_reset(message_header);
    tn_struct_read(message_header, protocol, transport, &error);
    switch (message_header->type) {
      case BOOMBOX_MESSAGE_TYPE_LAUNCH:
        tn_struct_read(launch_command, protocol, transport, &error);
        response = server_process_launch();
        break;
      case BOOMBOX_MESSAGE_TYPE_GET_STATE:
        tn_struct_read(state_command, protocol, transport, &error);
        response = server_process_get_state();
        break;        
      default:
        message_header->type = BOOMBOX_MESSAGE_TYPE_ERROR;
        response = error_response;
        break;
    }
    
    tn_object_reset(transport);
    tn_object_reset(protocol);
    Serial.print("1 POS=");
    Serial.println(buf->len);
    tn_struct_write(message_header, protocol, transport, &error);
    if (response != NULL) {
      tn_struct_write(response, protocol, transport, &error);
    }
    tn_transport_write(transport, magic_footer, 4, &error);
    Serial.print("2 POS=");
    Serial.println(buf->len);
    response_handler((const byte*) buf->buf, buf->len);

    tn_object_reset(transport);
    tn_object_reset(protocol);
    tn_object_reset(message_header);
  }

  return true;
}

