PYTHON?=python3
VENV?=venv
REQUIREMENTS?=requirements.txt

.PHONY: install reinstall

install:
	$(PYTHON) -m virtualenv $(VENV)
	. $(VENV)/bin/activate; $(PYTHON) -m pip install -r $(REQUIREMENTS); $(PYTHON) generate_plots.py

reinstall:
	rm -rf $(VENV)
	$(MAKE) install
