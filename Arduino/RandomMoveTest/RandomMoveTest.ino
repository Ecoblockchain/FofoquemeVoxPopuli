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

void setup() {
  panMotor.setup(PAN_PWM0, PAN_PWM1, PAN_SWITCH0, PAN_SWITCH1);
  tiltMotor.setup(TILT_PWM0, TILT_PWM1, TILT_SWITCH0, TILT_SWITCH1);

  PCintPort::attachInterrupt(PAN_SWITCH0, &onInterrupt, FALLING);
  PCintPort::attachInterrupt(PAN_SWITCH1, &onInterrupt, FALLING);
  PCintPort::attachInterrupt(TILT_SWITCH0, &onInterrupt, FALLING);
  PCintPort::attachInterrupt(TILT_SWITCH1, &onInterrupt, FALLING);
}

void loop() {
  panMotor.update();
  tiltMotor.update();
}

