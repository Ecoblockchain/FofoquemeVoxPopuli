#pragma once

class VoxMotor {
  enum StateType { 
    PAUSE, SPEED_UP, SPEED_DOWN  };
  int pin[2];
  int limit[2];
  short currentDirection;
  float currentDutyCycle;
  unsigned long changeStateMillis;
  StateType currentState;

public:
  VoxMotor();
  void setup(int motor0, int motor1, int switch0, int switch1);
  void stop();
  void update();
};

