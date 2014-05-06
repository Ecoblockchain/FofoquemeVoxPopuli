# -*- coding: utf-8 -*-

from time import time, sleep
from sys import exit

def setup():
    print "hello"

def loop():
	print "looop"

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
