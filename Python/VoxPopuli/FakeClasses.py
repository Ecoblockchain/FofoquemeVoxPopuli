class FakeAlsa:
	PCM_NORMAL = 0
	PCM_CAPTURE = 0
	PCM_FORMAT_S16_LE = 0

	def __init__(self):
		pass
	def PCM(capture, type, device):
		pass
	PCM = staticmethod(PCM)

class FakeGPIO:
	LOW = 0
	HIGH = 1
	IN = 0
	OUT = 0
	BCM = 0
	PUD_DOWN = 0

	def __init__(self):
		pass
	def setmode(mode):
		pass
	def setup(pin, mode, pull_up_down=''):
		pass
	def input(pin):
		pass
	def output(pin, value):
		pass
	def cleanup():
		pass
	setmode = staticmethod(setmode)
	setup = staticmethod(setup)
	input = staticmethod(input)
	output = staticmethod(output)
	cleanup = staticmethod(cleanup)
