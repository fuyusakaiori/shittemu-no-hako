package logger

import (
	"fmt"
	"io"
	"log"
	"os"
	"path"
	"runtime"
	"sync"
	"time"
)

type LogLevel int

// 日志等级
const (
	DEBUG  LogLevel = 0
	INFO   LogLevel = 1
	WARING LogLevel = 2
	ERROR  LogLevel = 3
	FATAL  LogLevel = 4
)

// 日志默认配置
const (
	defaultFlags       int = log.LstdFlags // 默认的日志输出的格式
	defaultCallerDepth int = 2
	defaultBufferSize  int = 1e5 // 默认的日志缓冲区大小
)

var levels = []string{"DEBUG", "INFO", "WARING", "ERROR", "FATAL"}

var DefaultLogger ILogger = NewStdoutLogger()

// 日志配置
type Setting struct {
	Path       string `yaml:"path"`        // 日志路径
	Name       string `yaml:"name"`        // 日志名称
	Ext        string `yaml:"ext"`         // 日志文件类型
	TimeFormat string `yaml:"time-format"` // 日志时间格式
}

type ILogger interface {
	Output(level LogLevel, callerDepth int, message string)
}

type Logger struct {
	logFile      *os.File       // 日志文件
	logger       *log.Logger    // 日志输出类
	logEntryChan chan *logEntry // 日志消息
	logEntryPool *sync.Pool     // 日志对象缓存池
}

type logEntry struct {
	message string
	level   LogLevel
}

// 初始化标准日志类
func NewStdoutLogger() ILogger {
	// 1. 初始化日志
	logger := &Logger{
		logFile:      nil,
		logger:       log.New(os.Stdout, "", defaultFlags),
		logEntryChan: make(chan *logEntry, defaultBufferSize),
		logEntryPool: &sync.Pool{
			New: func() interface{} {
				return &logEntry{}
			},
		},
	}
	// 2. 启动协程异步记录日志
	go func() {
		// 3. 循环从 channel 中获取对应的日志信息
		for entry := range logger.logEntryChan {
			// 3.1 记录日志
			_ = logger.logger.Output(0, entry.message)
			// 3.2 日志放入缓存池
			logger.logEntryPool.Put(entry)
		}
	}()

	return logger
}

// 初始化文件日志类
func NewFileLogger(setting *Setting) (ILogger, error)  {
	// 1. 初始化日志文件名称
	fileName := fmt.Sprintf("%s-%s.%s",
		setting.Name, time.Now().Format(setting.TimeFormat), setting.Ext)
	// 2. 打开日志文件
	file, err := mustOpenFile(fileName, setting.Path)
	// 3. 判断是否打开成功
	if err != nil {
		return nil, err
	}
	// 4. 初始化日志类
	logger := &Logger{
		logFile:      file,
		logger:       log.New(io.MultiWriter(os.Stdout, file), "", defaultFlags), // 同时支持标准输出和文件输出
		logEntryChan: make(chan *logEntry, defaultBufferSize),
		logEntryPool: &sync.Pool{
			New: func() interface{} {
				return &logEntry{}
			},
		},
	}
	// 5. 开启协程打印日志
	go func() {
		for entry := range logger.logEntryChan {
			// 5.1 获取当前的日志文件名称
			currentFileName := fmt.Sprintf("%s-%s.%s",
				setting.Name, time.Now().Format(setting.TimeFormat), entry.message)
			// 5.2 判断日志是否需要滚动: 理论上应该是日志写满才滚动, 这里应该是方便处理直接根据时间戳滚动
			if path.Join(setting.Path, currentFileName) != logger.logFile.Name() {
				// 5.2.1 重新打开新的日志文件
				file, err := mustOpenFile(currentFileName, setting.Path)
				if err != nil {
					panic(fmt.Sprintf("open new log file %s err %s", currentFileName, err))
				}
				// 5.2.2 更新日志类
				logger.logFile = file
				logger.logger = log.New(io.MultiWriter(os.Stdout, file), "", defaultFlags)
			}
			// 5.3 如果不需要滚动, 就直接写入日志
			_ = logger.logger.Output(0, entry.message)
			// 5.4 放入缓存池
			logger.logEntryPool.Put(entry)
		}
	}()

	return logger, nil
}

// 设置文件日志类的配置
func SetUpFileLogger(setting *Setting) error {
	logger, err := NewFileLogger(setting)
	if err != nil {
		return err
	}
	DefaultLogger = logger
	return nil
}

func Debug(logs ...interface{})  {
	message := fmt.Sprintln(logs...)
	DefaultLogger.Output(DEBUG, defaultCallerDepth, message)
}

func Debugf(format string, logs ...interface{})  {
	message := fmt.Sprintf(format, logs...)
	DefaultLogger.Output(DEBUG, defaultCallerDepth, message)
}

func Info(logs ...interface{})  {
	message := fmt.Sprintln(logs...)
	DefaultLogger.Output(INFO, defaultCallerDepth, message)
}

func Infof(format string, logs ...interface{})  {
	message := fmt.Sprintf(format, logs...)
	DefaultLogger.Output(INFO, defaultCallerDepth, message)
}

func Warning(logs ...interface{})  {
	message := fmt.Sprintln(logs...)
	DefaultLogger.Output(WARING, defaultCallerDepth, message)
}

func Warningf(format string, logs ...interface{})  {
	message := fmt.Sprintf(format, logs...)
	DefaultLogger.Output(WARING, defaultCallerDepth, message)
}

func Error(logs ...interface{})  {
	message := fmt.Sprintln(logs...)
	DefaultLogger.Output(ERROR, defaultCallerDepth, message)
}

func Errorf(format string, logs ...interface{})  {
	message := fmt.Sprintf(format, logs...)
	DefaultLogger.Output(ERROR, defaultCallerDepth, message)
}

func Fatal(logs ...interface{})  {
	message := fmt.Sprintln(logs...)
	DefaultLogger.Output(FATAL, defaultCallerDepth, message)
}

func Fatalf(format string, logs ...interface{})  {
	message := fmt.Sprintf(format, logs...)
	DefaultLogger.Output(FATAL, defaultCallerDepth, message)
}

func (logger *Logger) Output(level LogLevel, callerDepth int, message string) {
	formatMessage := ""
	// 1. 获取栈帧信息
	_, file, line, ok := runtime.Caller(callerDepth)
	// 2. 判断是否获取成功
	if ok {
		formatMessage = fmt.Sprintf("[%s] [%s:%d] %s]", levels[level], file, line, message)
	} else {
		formatMessage = fmt.Sprintf("[%s] %s", levels[level], message)
	}
	// 3. 获取缓存池中的日志消息
	entry := logger.logEntryPool.Get().(*logEntry)
	// 4. 设置日志类消息
	entry.level = level
	entry.message = formatMessage
	logger.logEntryChan <- entry
}


