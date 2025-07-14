package main

import (
	"bufio"
	"fmt"
	"io"
	"log"
	"net"
)

// 监听连接
func ListenAndServe(address string) {
    // 1. 绑定监听地址
	listener, err := net.Listen("tcp", address)
	// 2. 判断是否初始化监听其成功
	if err != nil {
		log.Fatalln(fmt.Sprintf("listener err: %v", err))
	}
	log.Println(fmt.Sprintf("listener bind: %v, start listening...", address))
	// 3. 服务器终止时关闭监听器
	defer listener.Close()
	// 4. 无限循环避免监听器结束
	for {
		// 4.1 阻塞监听建立的连接
		connection, err := listener.Accept()
		// 4.2 判断是否监听连接成功
		if err != nil {
			log.Fatalln(fmt.Sprintf("listener accept err: %v", err))
		}
		// 4.3 协程处理新建立的连接
		go Handle(connection)
	}
}

// 处理链接
func Handle(connection net.Conn)  {
    // 1. 初始化连接的 reader
	reader := bufio.NewReader(connection)
	// 2. 循环读取连接中的数据
	for {
		// 2.1 reader 会持续读取数据直到遇到分隔符为止, 返回的数据会包括分隔符本身
		message, err := reader.ReadString('\n')
		// 2.2 判断读取数据是否出现异常
		if err != nil {
			if err != io.EOF {
				log.Println(fmt.Sprintf(
					"listener %v connection close", connection.RemoteAddr().String()))
			} else {
				log.Println(fmt.Sprintf(
					"listener read %v connection occurred err: %v", connection.RemoteAddr().String(), err))
			}
			return
		}
		// 2.3 回写给客户端
		if _, err := connection.Write([]byte(message)); err != nil {
			log.Println(fmt.Sprintf(
				"listener write %v connection occurred err: %v", connection.RemoteAddr().String(), err))
		}
	}
}

func main() {
    ListenAndServe("localhost:8080")
}
