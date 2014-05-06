# -*- coding: utf-8 -*-

from time import time, sleep
from sys import exit
import Adafruit_BBIO.GPIO as GPIO

LED_PIN = "P8_7"
SWITCH_PIN = "P8_8"

def setup():
	global currentButtonState, lastDownTime, isRecording
	GPIO.setup(SWITCH_PIN, GPIO.IN)
	GPIO.setup(LED_PIN, GPIO.OUT)
	GPIO.output(LED_PIN, GPIO.LOW)
	currentButtonState = GPIO.input(SWITCH_PIN)
	lastDownTime = 0
	isRecording = False

def loop():
	global currentButtonState, lastDownTime, isRecording

	previousButtonState = currentButtonState
	currentButtonState = GPIO.input(SWITCH_PIN)
	buttonJustGotPressed = (currentButtonState is GPIO.HIGH and previousButtonState is GPIO.LOW)
	buttonJustGotReleased = (currentButtonState is GPIO.LOW and previousButtonState is GPIO.HIGH)
	if buttonJustGotPressed:
		lastDownTime = time()

	if (isRecording):
		if((time()-lastDownTime > 8.0) or
			(buttonJustGotReleased and (time()-lastDownTime > 1.0)) or
			buttonJustGotPressed):
			isRecording = False
			## TODO: stop recording
	elif buttonJustGotPressed:
			isRecording = True
			## TODO: start recording

	GPIO.output(LED_PIN, GPIO.HIGH if isRecording else GPIO.LOW)

if __name__=="__main__":
	setup()

	try:
		while(True):
			## keep it from looping faster than ~60 times per second
			loopStart = time()
			loop()
			loopTime = time()-loopStart
			if (loopTime < 0.016):
				sleep(0.016 - loopTime)
		exit(0)
	except KeyboardInterrupt:
		GPIO.cleanup()
		exit(0)
