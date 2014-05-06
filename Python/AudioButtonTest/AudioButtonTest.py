# -*- coding: utf-8 -*-

from time import time, sleep
from sys import exit
import threading
import Adafruit_BBIO.GPIO as GPIO
import alsaaudio

LED_PIN = "P8_7"
SWITCH_PIN = "P8_8"

class RecordThread(threading.Thread):
	def __init__(self):
		super(RecordThread, self).__init__()
		self.audioFile = open("foo.wav", 'wb')

	def run(self):
		while(isRecording):
			l, data = audioInput.read()
			if l:
				self.audioFile.write(data)
				sleep(0.001)
		self.audioFile.close()

def setup():
	global currentButtonState, lastDownTime, isRecording, audioInput
	GPIO.setup(SWITCH_PIN, GPIO.IN)
	GPIO.setup(LED_PIN, GPIO.OUT)
	GPIO.output(LED_PIN, GPIO.LOW)
	currentButtonState = GPIO.input(SWITCH_PIN)
	lastDownTime = 0
	isRecording = False
	audioInput = alsaaudio.PCM(alsaaudio.PCM_CAPTURE, alsaaudio.PCM_NORMAL, "default:Headset")

	# Set attributes
	audioInput.setchannels(1)
	audioInput.setrate(44100)
	audioInput.setformat(alsaaudio.PCM_FORMAT_S16_LE)
	audioInput.setperiodsize(256)

def loop():
	global currentButtonState, lastDownTime, isRecording, myThread

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
			myThread.join()
	elif buttonJustGotPressed:
			isRecording = True
			myThread = RecordThread()
			myThread.start()

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
