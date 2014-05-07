# -*- coding: utf-8 -*-

from time import time, sleep
from threading import Thread
from Queue import Queue
from socket import gethostname
from OSC import OSCClient, OSCMessage, OSCServer, getUrlStr, OSCClientError


OSC_IN_ADDRESS = gethostname()+".local"
OSC_IN_PORT = 8888

def _oscHandler(self, addr, tags, stuff, source):
	addrTokens = addr.lstrip('/').split('/')

	if (addrTokens[0].lower() == "ffqmesms"):
		messageQ.put(stuff[0].decode('utf-8'))
	elif (addrTokens[0].lower() == "ffqmeping"):
		ip = getUrlStr(source).split(":")[0]
		port = stuff[0]
		clientMap[(ip,port)] = time()

def setup():
	global messageQ, clientMap, oscIn, oscOut, oscThread
	messageQ = Queue()
	clientMap = {}

	## setup osc client
	oscOut = OSCClient()

	## setup osc receiver
	oscIn = OSCServer((OSC_IN_ADDRESS, OSC_IN_PORT))
	oscIn.addMsgHandler('default', _oscHandler)
	oscThread = Thread(target = oscIn.serve_forever)
	oscThread.start()

def loop():
	global messageQ, clientMap, oscOut

	if(not messageQ.empty()):
		# TODO change this to something more complicated...
		# TODO nltk
		msg = messageQ.get()
		for (i,p) in clientMap:
			if(time()-clientMap[(i,p)] < 60):
				oscMsg = OSCMessage()
				oscMsg.setAddress("/ffqmevox")
				oscMsg.append(msg)
				try:
					oscOut.connect((i,p))
					oscOut.sendto(msg, (i,p))
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
