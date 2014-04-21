#include <PinChangeInt.h>

#define NO_PORTB_PINCHANGES
#define NO_PORTC_PINCHANGES
#define NO_PORTD_PINCHANGES
#define NO_PORTJ_PINCHANGES
#define NO_PIN_STATE

#define PAN_PWM0 2
#define PAN_PWM1 3
#define TILT_PWM0 4
#define TILT_PWM1 5

#define PAN_SWITCH0 A10
#define PAN_SWITCH1 A11
#define TILT_SWITCH0 A8
#define TILT_SWITCH1 A9

#define PWM_MAX_DUTY 200

int panDirection, tiltDirection;
float currentPanDutyCycle, currentTiltDutyCycle;

void onInterrupt(){
  if(PCintPort::arduinoPin == PAN_SWITCH0){
    panDirection = -1;
    currentPanDutyCycle = 0;
  }
  else if(PCintPort::arduinoPin == PAN_SWITCH1){
    panDirection = 1;
    currentPanDutyCycle = 0;
  }
  else if(PCintPort::arduinoPin == TILT_SWITCH0){
    tiltDirection = -1;
    currentTiltDutyCycle = 0;
  }
  else if(PCintPort::arduinoPin == TILT_SWITCH1){
    tiltDirection = 1;
    currentTiltDutyCycle = 0;
  }
}

void setup() {
  pinMode(PAN_PWM0, OUTPUT);
  pinMode(PAN_PWM1, OUTPUT);
  pinMode(TILT_PWM0, OUTPUT);
  pinMode(TILT_PWM1, OUTPUT);

  pinMode(PAN_SWITCH0, INPUT_PULLUP);
  pinMode(PAN_SWITCH1, INPUT_PULLUP);
  pinMode(TILT_SWITCH0, INPUT_PULLUP);
  pinMode(TILT_SWITCH1, INPUT_PULLUP);

  PCintPort::attachInterrupt(PAN_SWITCH0, &onInterrupt, FALLING);
  PCintPort::attachInterrupt(PAN_SWITCH1, &onInterrupt, FALLING);
  PCintPort::attachInterrupt(TILT_SWITCH0, &onInterrupt, FALLING);
  PCintPort::attachInterrupt(TILT_SWITCH1, &onInterrupt, FALLING);

  analogWrite(PAN_PWM0, 0);
  analogWrite(PAN_PWM1, 0);
  analogWrite(TILT_PWM0, 0);
  analogWrite(TILT_PWM1, 0);

  panDirection = tiltDirection = 1;
  currentPanDutyCycle = currentTiltDutyCycle = 0;
}

void loop() {
  analogWrite(PAN_PWM0, (panDirection>0)?currentPanDutyCycle:0);
  analogWrite(PAN_PWM1, (panDirection>0)?0:currentPanDutyCycle);

  analogWrite(TILT_PWM0, (tiltDirection>0)?currentTiltDutyCycle:0);
  analogWrite(TILT_PWM1, (tiltDirection>0)?0:currentTiltDutyCycle);

  currentPanDutyCycle += (currentPanDutyCycle<PWM_MAX_DUTY)?0.05:0;
  currentTiltDutyCycle += (currentTiltDutyCycle<PWM_MAX_DUTY)?0.05:0;
}



