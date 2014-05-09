#include <adk.h>
#include <PinChangeInt.h>
#include "VoxMotor.h"

#define NO_PORTB_PINCHANGES
#define NO_PORTC_PINCHANGES
#define NO_PORTD_PINCHANGES
#define NO_PORTJ_PINCHANGES
#define NO_PIN_STATE

#define PAN_PWM0 2
#define PAN_PWM1 3
#define PAN_SWITCH0 A10
#define PAN_SWITCH1 A11

#define TILT_PWM0 4
#define TILT_PWM1 5
#define TILT_SWITCH0 A8
#define TILT_SWITCH1 A9

VoxMotor panMotor, tiltMotor;

void onInterrupt(){
  if((PCintPort::arduinoPin == PAN_SWITCH0) || (PCintPort::arduinoPin == PAN_SWITCH1)){
    panMotor.stop();
  }
  else if((PCintPort::arduinoPin == TILT_SWITCH0) || (PCintPort::arduinoPin == TILT_SWITCH1)){
    tiltMotor.stop();
  }
}


USB Usb;
ADK adk(&Usb, "Arduino", "ADK", "Description", "1.0", "http://fofoque.me", "0000000012345678");

void setup() {
  Serial.begin(115200);
  pinMode(13,OUTPUT);
  digitalWrite(13,LOW);

  if (Usb.Init() == -1) {
    Serial.print("\r\nOSCOKIRQ failed to assert");
    while (1); // halt
  }
  Serial.print("\r\nVox Populi Started");
  panMotor.setup(PAN_PWM0, PAN_PWM1, PAN_SWITCH0, PAN_SWITCH1);
  tiltMotor.setup(TILT_PWM0, TILT_PWM1, TILT_SWITCH0, TILT_SWITCH1);

  PCintPort::attachInterrupt(PAN_SWITCH0, &onInterrupt, FALLING);
  PCintPort::attachInterrupt(PAN_SWITCH1, &onInterrupt, FALLING);
  PCintPort::attachInterrupt(TILT_SWITCH0, &onInterrupt, FALLING);
  PCintPort::attachInterrupt(TILT_SWITCH1, &onInterrupt, FALLING);
}

void loop() {
  Usb.Task();
  if (adk.isReady()) {
    uint8_t msg[4];
    uint16_t len = sizeof(msg);
    uint8_t rcode = adk.RcvData(&len, msg);
    if (rcode && rcode != hrNAK) {
      Serial.print(F("\r\nrcode rcv: "));
      Serial.print(rcode, HEX);
    }
    else if (len > 3) {
      Serial.print(F("\r\nGot Data Packet"));
      if((msg[0] == (byte)0xff) && (msg[1] == (byte)0x93)){
        panMotor.setTarget(msg[2]);
        tiltMotor.setTarget(msg[3]);
      }
      else if((msg[0] == (byte)0xff) && (msg[1] == (byte)0x22)){
        digitalWrite(13, msg[2] ? HIGH : LOW);
      }
    }
  }

  panMotor.update();
  tiltMotor.update();

  if(!(panMotor.isDone() || tiltMotor.isDone())){
    byte doneSignal = 0xf9;
    uint8_t rcode = adk.SndData(sizeof(doneSignal), (uint8_t*)&doneSignal);
    panMotor.goWait();
    tiltMotor.goWait();
  }
}

