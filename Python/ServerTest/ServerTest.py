from threading import Thread
from SimpleHTTPServer import SimpleHTTPRequestHandler
from SocketServer import TCPServer

class ThreadedServer(Thread):
	def __init__(self):
		super(ThreadedServer, self).__init__()
		self.keepServing = True

	def run(self):
		self.httpd = TCPServer(('', 8000), SimpleHTTPRequestHandler)
		while(self.keepServing):
			#httpd.serve_forever()
			self.httpd.handle_request()
		print "done serving"

	def stop(self):
		self.keepServing = False

mServer = ThreadedServer()
mServer.start()
## do some stuff
#mServer.stop()
