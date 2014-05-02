#include <adk.h>

#define LED 13

USB Usb;
ADK adk(&Usb, "Arduino", "ADK", "Description", "1.0", "http://fofoque.me", "0000000012345678");

void setup() {
  Serial.begin(115200);
  if (Usb.Init() == -1) {
    Serial.print("\r\nOSCOKIRQ failed to assert");
    while (1); // halt
  }
  pinMode(LED, OUTPUT);
  Serial.print("\r\nArduino ADK Test Started");
}

void loop() {
  Usb.Task();

  if (adk.isReady()) {
    uint8_t msg[1];
    uint16_t len = sizeof(msg);
    uint8_t rcode = adk.RcvData(&len, msg);
    if (rcode && rcode != hrNAK) {
      Serial.print(F("\r\nData rcv: "));
      Serial.print(rcode, HEX);
    }
    else if (len > 0) {
      Serial.print(F("\r\nData Packet: "));
      Serial.print(msg[0]);
      digitalWrite(LED, msg[0] ? HIGH : LOW);
    }
  }
}

