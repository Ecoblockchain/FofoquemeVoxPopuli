#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>
#define  LED_PIN  13

AndroidAccessory acc("Arduino", "ADK", "Description", "1.0", "http://fofoque.me", "0000000012345678");

void setup() {
  // set communiation speed
  Serial.begin(115200);
  pinMode(LED_PIN, OUTPUT);
  acc.powerOn();
}

void loop() {
  byte msg[0];
  if (acc.isConnected()) {
    int len = acc.read(msg, sizeof(msg), 1);
    if (len > 0) {
      digitalWrite(LED_PIN, msg[0]?HIGH:LOW);
    }
  }
}


