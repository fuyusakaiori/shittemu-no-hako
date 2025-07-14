package logger

import (
	"fmt"
	"os"
)

// 打开文件
func mustOpenFile(fileName string, directory string) (*os.File, error)  {
	// 1. 检查目录权限
	if !checkPermission(directory) {
		return nil, fmt.Errorf("permission denied directory: %s", directory)
	}
	// 2. 检查目录是否存在, 不存在的话就创建目录
	if err := isNotExistMakeDir(directory); err != nil {
		return nil, fmt.Errorf("make directory %v, err %v", directory, err)
	}
	// 3. 打开文件
	file, err := os.OpenFile(directory + string(os.PathSeparator) + fileName, os.O_RDWR | os.O_CREATE | os.O_APPEND, 0644)
	// 4. 判断是否打开成功
	if err != nil {
		return nil, fmt.Errorf("open file %v, err %v", fileName, err)
	}
	return file, nil
}

// 检查文件权限
func checkPermission(src string) bool  {
	// 1. 获取文件的状态信息
	_, err := os.Stat(src)
	// 2. 判断是否为没有权限的错误
	return os.IsPermission(err)
}

// 如果目录不存在就创建目录
func isNotExistMakeDir(directory string) error {
	// 1. 判断文件是否存在
	if !checkExist(directory) {
		return makeDir(directory)
	}
	return nil
}

// 检查文件是否存在
func checkExist(src string) bool  {
	// 1. 获取文件的状态信息
	_, err := os.Stat(src)
	// 2. 判断是否为不存在的错误
	return os.IsExist(err)
}

// 创建目录
func makeDir(directory string) error  {
	return os.MkdirAll(directory, os.ModePerm)
}
