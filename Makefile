UNAME := $(shell uname)
CCFLAGS = -o deptracker.o -I${JAVA_HOME}/include -c -fPIC -fpermissive
ifeq ($(UNAME), Linux)
	CCFLAGS += -I${JAVA_HOME}/include/linux 
	LINKFLAGS = -z defs -static-libgcc -shared -o target/libdeptracker.so -lc 
endif
ifeq ($(UNAME), Darwin)
	CCFLAGS += -I${JAVA_HOME}/include/darwin
	LINKFLAGS += -dynamiclib -o target/libdeptracker.so
endif

tracker.so:
	gcc ${CCFLAGS} src/main/c/deptracker.cpp -o target/deptracker.o
	g++ ${LINKFLAGS} target/deptracker.o
clean:
	rm -rf target/deptracker.o target/libdeptracker.so target/libdeptracker.so
