#pragma once
#include "Arduino.h"

class VoxMotor {
  enum StateType { 
    WAIT, SPEED_UP, SPEED_DOWN, DONE };
  int pin[2];
  int limit[2];
  short currentDirection;
  float currentDutyCycle;
  unsigned long changeStateMillis;
  unsigned long rampDurationMillis;
  StateType currentState;

public:
  VoxMotor(int motor0, int motor1, int switch0, int switch1);
  void stopAndChangeDirection();
  void update();
  boolean isDone();
  void goWait();
  void setTarget(uint8_t t);
};

