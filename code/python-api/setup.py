from setuptools import find_packages, setup

setup(
    name="book-club",
    version="0.0.1",
    python_requires=">3.7",
    packages=find_packages(),
    include_package_data=True,
    zip_safe=False,
    install_requires=[
        "fastapi",
        "pymysql",
        "pydantic",
        "SQLAlchemy",
        "jsonschema",
        "uvicorn"
    ],
    extras_require={
        "dev": ["pytest", "requests"]
    }
)
