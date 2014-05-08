# -*- coding: utf-8 -*-

# to run: echo passwd | sudo python VoxPopuli.py

from time import time, sleep
from sys import exit
from threading import Thread
from subprocess import call
import alsaaudio

class RecordThread(Thread):
	def __init__(self):
		super(RecordThread, self).__init__()
		self.audioFile = open("foo.raw", 'wb')

	def run(self):
		while(isRecording):
			l, data = audioInput.read()
			if l:
				self.audioFile.write(data)
				sleep(0.001)
		self.audioFile.close()

def setup():
	global isRecording, audioInput, startTime

	isRecording = False
	startTime = time()

	## setup audio
	audioInput = None
	try:
		audioInput = alsaaudio.PCM(alsaaudio.PCM_CAPTURE, alsaaudio.PCM_NORMAL, "default:Headset")
		audioInput.setchannels(1)
		audioInput.setrate(44100)
		audioInput.setformat(alsaaudio.PCM_FORMAT_S16_LE)
		audioInput.setperiodsize(256)
	except:
		print "couldn't start audio device"
		audioInput = None

def loop():
	global isRecording, audioThread, startTime

	if (time()-startTime > 10):
			isRecording = False
			audioThread.join()
			call('lame -mm -r foo.raw foo.mp3', shell=True)
			raise KeyboardInterrupt
	elif (not isRecording):
			isRecording = (not audioInput is None)
			audioThread = RecordThread()
			audioThread.start()

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
		exit(0)
