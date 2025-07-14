package tcp

import (
	"context"
	"net"
)

// 连接处理器
type Handler interface {
	// 连接处理
	Handle(context context.Context, connection net.Conn)
	// 连接关闭
	Close() error
}

// 监听连接
func ListenAndServe(listener net.Listener, handler Handler, closeChan <- chan struct{})  {
	// 1. 启动协程监听关闭信号
	go func() {
		<- closeChan // 阻塞监听关闭信号

	}()
}


