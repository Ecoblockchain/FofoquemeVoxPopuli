# -*- coding: utf-8 -*-

# to run: echo passwd | sudo python VoxPopuli.py
# to listen to mp3: mplayer -ao alsa:device=default=Headset vox.mp3

from time import time, sleep, strftime, localtime
from sys import exit
from threading import Thread
from random import randint
from subprocess import call
from Queue import PriorityQueue
from OSC import OSCClient, OSCMessage, OSCServer, getUrlStr, OSCClientError
from SimpleHTTPServer import SimpleHTTPRequestHandler
from SocketServer import TCPServer

try:
	import alsaaudio
except ImportError:
	from FakeClasses import FakeAlsa as alsaaudio

try:
	import RPi.GPIO as GPIO
except ImportError:
	from FakeClasses import FakeGPIO as GPIO

VOICE_MESSAGE_STRING = "!!!FFQMEVOXPOPULI!!!";
OSC_IN_ADDRESS = "127.0.0.1" #"200.0.0.101"
OSC_IN_PORT = 8888
HTTP_IN_PORT = 8666
LED_PIN = 7
SWITCH_PIN = 8

class RecordThread(Thread):
	def __init__(self):
		super(RecordThread, self).__init__()
		self.audioFile = open("vox.raw", 'wb')

	def run(self):
		while(isRecording):
			l, data = audioInput.read()
			if l:
				self.audioFile.write(data)
				sleep(0.001)
		self.audioFile.close()

class ThreadedServer(Thread):
	def __init__(self):
		super(ThreadedServer, self).__init__()
		TCPServer.allow_reuse_address = True
		self.httpd = TCPServer(('', HTTP_IN_PORT), SimpleHTTPRequestHandler)

	def run(self):
		self.httpd.serve_forever(poll_interval=0.5)
		print "done serving"

	def stop(self):
		self.httpd.shutdown()
		self.httpd.socket.close()

def _oscHandler(addr, tags, stuff, source):
	addrTokens = addr.lstrip('/').split('/')

	if (addrTokens[0].lower() == "ffqmesms"):
		ip = source[0]
		msg = stuff[0].decode('utf-8')
		print "got %s from %s" % (msg,ip)
		messageQ.put((5, msg))
	elif (addrTokens[0].lower() == "ffqmeping"):
		ip = source[0]
		port = stuff[0]
		print "got ping from %s %s" % (ip,port)
		clientMap[(ip,int(port))] = time()

def _setupAudio():
	global audioInput
	audioInput = None

	try:
		audioInput = alsaaudio.PCM(alsaaudio.PCM_CAPTURE, alsaaudio.PCM_NORMAL, "default:Headset")
		audioInput.setchannels(1)
		audioInput.setrate(44100)
		audioInput.setformat(alsaaudio.PCM_FORMAT_S16_LE)
		audioInput.setperiodsize(256)
	except Exception as e:
		print "couldn't start audio device: "+str(e)
		audioInput = None

def setup():
	print "setup"
	global currentButtonState, lastDownTime, isRecording, audioInput
	global messageQ, clientMap, oscIn, oscOut, oscThread, mAudioServer
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

	## setup audio server
	mAudioServer = ThreadedServer()
	mAudioServer.start()

	## setup gpio
	GPIO.setmode(GPIO.BCM)
	GPIO.setup(SWITCH_PIN, GPIO.IN, pull_up_down = GPIO.PUD_DOWN)
	GPIO.setup(LED_PIN, GPIO.OUT)
	GPIO.output(LED_PIN, GPIO.LOW)
	currentButtonState = GPIO.input(SWITCH_PIN)
	lastDownTime = 0
	isRecording = False

	_setupAudio()

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
			GPIO.output(LED_PIN, GPIO.LOW)
			audioThread.join()
			call('rm -rf vox.mp3', shell=True)
			call('lame -mm -r vox.raw vox.mp3', shell=True)
			call('cp vox.mp3 data/vox_'+strftime("%Y%m%d_%H%M%S", localtime())+'.mp3', shell=True)
			call('rm -rf vox.raw', shell=True)
			_setupAudio()
			messageQ.put((1, VOICE_MESSAGE_STRING))
	elif buttonJustGotPressed:
			isRecording = (not audioInput is None)
			GPIO.output(LED_PIN, GPIO.HIGH)
			audioThread = RecordThread()
			audioThread.start()

	## deal with messages
	if(not messageQ.empty()):
		# TODO change this to something more complicated...
		# TODO nltk
		msg = messageQ.get()[1]
		for (i,p) in clientMap:
			if(time()-clientMap[(i,p)] < 60):
				oscMsg = OSCMessage()
				oscMsg.setAddress("/ffqmevox")
				oscMsg.append(msg.encode('utf-8'))
				## TODO: pan and tilt and delay
				oscMsg.append(randint(0,255))
				oscMsg.append(randint(0,255))
				oscMsg.append(0)
				try:
					oscOut.connect((i,p))
					oscOut.sendto(oscMsg, (i,p))
					oscOut.connect((i,p))
				except OSCClientError:
					print "no connection to %s : %s, can't send message" % (i,p)

def cleanUp():
	oscIn.close()
	oscThread.join()
	mAudioServer.stop()
	mAudioServer.join()
	GPIO.cleanup()

if __name__=="__main__":
	setup()

	while(True):
		try:
			## keep it from looping faster than ~60 times per second
			loopStart = time()
			loop()
			loopTime = time()-loopStart
			if (loopTime < 0.016):
				sleep(0.016 - loopTime)
		except KeyboardInterrupt:
			cleanUp()
			exit(0)
		except Exception as e:
			print "loop caught: "+str(e)
			cleanUp()
			setup()
