# -*- coding: utf-8 -*-

# to run: echo passwd | sudo python VoxPopuli.py

from time import time, sleep
from sys import exit
from threading import Thread
from subprocess import call
from Queue import PriorityQueue
from OSC import OSCClient, OSCMessage, OSCServer, getUrlStr, OSCClientError
import Adafruit_BBIO.GPIO as GPIO
import alsaaudio

VOICE_MESSAGE_STRING = "!!!FFQMEVOXPOPULI!!!";
OSC_IN_ADDRESS = "voxserver.local"
OSC_IN_PORT = 8888
LED_PIN = "P8_7"
SWITCH_PIN = "P8_8"

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

def _oscHandler(addr, tags, stuff, source):
	addrTokens = addr.lstrip('/').split('/')
	print addrTokens

	if (addrTokens[0].lower() == "ffqmesms"):
		ip = getUrlStr(source).split(":")[0]
		msg = stuff[0].decode('utf-8')
		print "got %s from %s" % (msg,ip)
		messageQ.put((5, msg))
	elif (addrTokens[0].lower() == "ffqmeping"):
		ip = getUrlStr(source).split(":")[0]
		port = stuff[0]
		print "got ping from %s %s" % (ip,port)
		clientMap[(ip,int(port))] = time()

def setup():
	global currentButtonState, lastDownTime, isRecording, audioInput
	global messageQ, clientMap, oscIn, oscOut, oscThread
	messageQ = PriorityQueue()
	clientMap = {}

	## setup osc client
	oscOut = OSCClient()

	## setup osc receiver
	oscIn = OSCServer((OSC_IN_ADDRESS, OSC_IN_PORT))
	oscIn.addMsgHandler('default', _oscHandler)
	oscThread = Thread(target = oscIn.serve_forever)
	oscThread.start()
	print "osc in ready"

	## setup gpio
	GPIO.setup(SWITCH_PIN, GPIO.IN)
	GPIO.setup(LED_PIN, GPIO.OUT)
	GPIO.output(LED_PIN, GPIO.LOW)
	currentButtonState = GPIO.input(SWITCH_PIN)
	lastDownTime = 0
	isRecording = False

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
	global messageQ, clientMap, oscOut, currentButtonState, lastDownTime, isRecording, audioThread

	## deal with UI
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
			audioThread.join()
			call('lame -mm -r foo.raw foo.mp3', shell=True)
			messageQ.put((1, VOICE_MESSAGE_STRING))
	elif buttonJustGotPressed:
			isRecording = (not audioInput is None)
			audioThread = RecordThread()
			audioThread.start()

	GPIO.output(LED_PIN, GPIO.HIGH if isRecording else GPIO.LOW)

	## deal with messages
	if(not messageQ.empty()):
		# TODO change this to something more complicated...
		# TODO nltk
		msg = messageQ.get()[1]
		for (i,p) in clientMap:
			if(time()-clientMap[(i,p)] < 60):
				oscMsg = OSCMessage()
				oscMsg.setAddress("/ffqmevox")
				oscMsg.append(msg)
				## TODO: pan and tilt
				oscMsg.append(0)
				oscMsg.append(0)
				try:
					oscOut.connect((i,p))
					oscOut.sendto(oscMsg, (i,p))
					oscOut.connect((i,p))
				except OSCClientError:
					print "no connection to %s : %s, can't send message" % (i,p)

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
		oscIn.close()
		oscThread.join()
		exit(0)
