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

VoxMotor panMotor(PAN_PWM0, PAN_PWM1, PAN_SWITCH0, PAN_SWITCH1);
VoxMotor tiltMotor(TILT_PWM0, TILT_PWM1, TILT_SWITCH0, TILT_SWITCH1);

void onInterrupt(){
  if((PCintPort::arduinoPin == PAN_SWITCH0) || (PCintPort::arduinoPin == PAN_SWITCH1)){
    panMotor.stopAndChangeDirection();
  }
  else if((PCintPort::arduinoPin == TILT_SWITCH0) || (PCintPort::arduinoPin == TILT_SWITCH1)){
    tiltMotor.stopAndChangeDirection();
  }
}

void setup() {
  Serial.begin(57600);
  pinMode(13,OUTPUT);
  digitalWrite(13,LOW);

  PCintPort::attachInterrupt(PAN_SWITCH0, &onInterrupt, FALLING);
  PCintPort::attachInterrupt(PAN_SWITCH1, &onInterrupt, FALLING);
  PCintPort::attachInterrupt(TILT_SWITCH0, &onInterrupt, FALLING);
  PCintPort::attachInterrupt(TILT_SWITCH1, &onInterrupt, FALLING);

  Serial.print("\r\nVox Populi Started");
}

void loop() {
  if(Serial.available() > 3){
    byte header[2];
    header[0] = Serial.read();
    header[1] = Serial.read();
    if((header[0] == (byte)0xff) && (header[1] == (byte)0x93)){
      panMotor.setTarget(Serial.read());
      tiltMotor.setTarget(Serial.read());
    }
    else if((header[0] == (byte)0xff) && (header[1] == (byte)0x22)){
      digitalWrite(13, Serial.read() ? HIGH : LOW);
      Serial.read();
    }
  }

  panMotor.update();
  tiltMotor.update();

  if(panMotor.isDone() && tiltMotor.isDone()){
    Serial.write(0xf9);
    panMotor.goWait();
    tiltMotor.goWait();
  }
}

