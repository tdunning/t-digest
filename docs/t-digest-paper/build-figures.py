import time
import re
import sys
from glob import glob
from subprocess import check_output

logFile = open("figure.log", "a")

def log(s):
    now = time.strftime("%v %T %Z")
    logLine = "\t".join([now, s, '\n'])
    logFile.write(logLine)
    sys.stdout.write(logLine)

log('Starting figure run')
for script in glob('*.r'):
    t0 = time.time()
    output = check_output(["Rscript", script])
    t1 = time.time() 
    log('%(script)s took %(delta).1fs <%(output)s>' % {
        "script":script,
        "delta": (t1-t0),
        "output": re.sub("null device\s*\n\s+1\s?\n", "", output)})
