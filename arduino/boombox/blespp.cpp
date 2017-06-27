
#include "blespp.h"
#include <CurieBLE.h>

bool blespp_open_flag = false;
io_handler request_handler;
reset_handler rset_handler;
BLEService sppService("e079c6a0-aa8b-11e3-a903-0002a5d5c51b");
BLECharacteristic rxBuffer("b38312c0-aa89-11e3-9cef-0002a5d5c51b", BLEWrite | BLERead | BLENotify | BLEIndicate, 20);

void on_ble_connected(BLEDevice central) {
  Serial.print("Connected: ");
  Serial.println(central.address());
  rset_handler();
}

void on_ble_disconnected(BLEDevice central) {
  Serial.print("Disconnected: ");
  Serial.println(central.address());
}

void on_ble_characteristic_buffer_changed(BLEDevice central, BLECharacteristic in) {
  request_handler(in.value(), in.valueLength());
}

bool blespp_init(io_handler h, reset_handler h2) {
  request_handler = h;
  rset_handler = h2;
  BLE.setLocalName("BoomBox");
  BLE.setAdvertisedServiceUuid(sppService.uuid());
  sppService.addCharacteristic(rxBuffer);
  BLE.addService(sppService);
  BLE.setEventHandler(BLEConnected, on_ble_connected);
  BLE.setEventHandler(BLEDisconnected, on_ble_disconnected);
  rxBuffer.setEventHandler(BLEWritten, on_ble_characteristic_buffer_changed);
  return true;
}

bool blespp_is_open() {
  return blespp_open_flag;
}

void blespp_open() {
  BLE.begin();
  BLE.advertise();
  blespp_open_flag = true;
}

void blespp_close() {
  BLE.stopAdvertise();
  BLE.disconnect();
  blespp_poll();
  blespp_open_flag = false;
}

bool blespp_send(const byte* msg, unsigned int len) {
  return rxBuffer.writeValue(msg, len);
}

void blespp_poll() {
  if (blespp_open_flag) {
    BLE.poll();  
  }
}


