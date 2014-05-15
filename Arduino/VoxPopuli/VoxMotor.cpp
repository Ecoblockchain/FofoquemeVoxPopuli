#include "VoxMotor.h"

#define PWM_MAX_DUTY 0.4

VoxMotor::VoxMotor(int motor0, int motor1, int switch0, int switch1){
  // assume pwm'ing pin[0] will trigger limit[0]
  pin[0] = motor0;
  pin[1] = motor1;
  limit[0] = switch0;
  limit[1] = switch1;

  pinMode(pin[0], OUTPUT);
  pinMode(pin[1], OUTPUT);
  pinMode(limit[0], INPUT_PULLUP);
  pinMode(limit[1], INPUT_PULLUP);

  currentDirection = 0;
  currentDutyCycle = 0.0;
  currentState = DONE;

  if(digitalRead(limit[0]) == LOW){
    currentDirection = 1;
  }
}

boolean VoxMotor::isDone() {
  return (currentState == DONE);
}
void VoxMotor::goWait() {
  currentState = WAIT;
}

void VoxMotor::setTarget(uint8_t t) {
  if(currentState != WAIT){
    return;
  }

  currentDutyCycle = 0.0;
  rampDurationMillis = 2000; //map(abs(targetPosition - currentPosition), 0,255, 1000, 2000);
  changeStateMillis = millis()+rampDurationMillis;
  currentState = SPEED_UP;
}

void VoxMotor::update() {
  if(digitalRead(limit[0]) == LOW){
    currentDirection = 1;
  }
  if(digitalRead(limit[1]) == LOW){
    currentDirection = 0;
  }

  if((currentState == DONE) || (currentState == WAIT)){
    currentDutyCycle = 0.0;
  }
  if(currentState == SPEED_UP){
    currentDutyCycle += (currentDutyCycle<PWM_MAX_DUTY)?0.1:0;
    if(millis() > changeStateMillis){
      changeStateMillis = millis()+rampDurationMillis;
      currentState = SPEED_DOWN;
    }
  }
  if(currentState == SPEED_DOWN){
    currentDutyCycle -= (currentDutyCycle>0.1)?0.1:0;
    if(millis() > changeStateMillis){
      currentState = DONE;
    }
  }

  // motor update
  analogWrite(pin[0], (currentDirection==0)?(1.0-currentDutyCycle)*255.0:255);
  analogWrite(pin[1], (currentDirection==1)?(1.0-currentDutyCycle)*255.0:255);
}

