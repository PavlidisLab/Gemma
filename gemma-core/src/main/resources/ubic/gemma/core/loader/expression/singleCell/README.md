This package requires some Python module, in particular:

- anndata
- scipy

```
python -m venv create /space/opt/virtualenvs/gemma
source /space/opt/virtualenvs/gemma/bin/activate
pip install -f requirements.txt
```

Then, in your `Gemma.properties`, point to the Python from the virtual environment:

```
python.exe=/space/opt/virtualenvs/gemma/bin/python
```