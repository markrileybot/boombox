
#include "blespp.h"
#include <CurieBLE.h>

io_handler request_handler;
reset_handler rset_handler;
BLEService sppService("e079c6a0-aa8b-11e3-a903-0002a5d5c51b");
BLECharacteristic rxBuffer("b38312c0-aa89-11e3-9cef-0002a5d5c51b", BLEWrite | BLERead | BLENotify | BLEIndicate, 20);

void on_ble_connected(BLEDevice central) {
  Serial.print("Connected: ");
  Serial.println(central.address());
  BLE.stopAdvertise();
  rset_handler();
}

void on_ble_disconnected(BLEDevice central) {
  Serial.print("Disconnected: ");
  Serial.println(central.address());
  BLE.advertise();
}

void on_ble_characteristic_buffer_changed(BLEDevice central, BLECharacteristic in) {
  request_handler(in.value(), in.valueLength());
}

bool blespp_init(io_handler h, reset_handler h2) {
  request_handler = h;
  rset_handler = h2;
  BLE.begin();
  BLE.setLocalName("BoomBox");
  BLE.setAdvertisedServiceUuid(sppService.uuid());
  sppService.addCharacteristic(rxBuffer);
  BLE.addService(sppService);
  BLE.setEventHandler(BLEConnected, on_ble_connected);
  BLE.setEventHandler(BLEDisconnected, on_ble_disconnected);
  rxBuffer.setEventHandler(BLEWritten, on_ble_characteristic_buffer_changed);
  BLE.advertise();
  return true;
}

bool blespp_send(const byte* msg, unsigned int len) {
  Serial.print("subscribed=");
  Serial.println(rxBuffer.subscribed());
  return rxBuffer.writeValue(msg, len);
}

