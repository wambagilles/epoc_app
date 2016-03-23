VPATH=../../../resources/

all:EnergyAwareApplicationScheduling.pdf

EnergyAwareApplicationScheduling.pdf:EnergyAwareApplicationScheduling.tex
	pdflatex -output-directory=../../../resources $^

clean:
	rm -f ../../../resources/*.aux ../../../resources/*.log *~
