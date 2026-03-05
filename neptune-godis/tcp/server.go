package tcp

import (
	"context"
	"neptune-godis/lib/logger"
	"net"
	"sync"
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
	defer func() {
		_ = listener.Close()
		_ = handler.Close()
	}()
	// 1. 启动协程监听关闭信号
	go func() {
		<- closeChan // 阻塞监听关闭信号
		logger.Info("server shutting down...")
		// 1.1 停止监听连接
		_ = listener.Close()
		// 1.2 关闭连接的处理器
		_ = handler.Close()
	}()
	ctx := context.Background()
	// 2. 初始化信号量
	var waitGroup sync.WaitGroup
	// 3. 无限循环
	for  {
		// 3.1 监听连接
		connection, err := listener.Accept()
		if err != nil {
			logger.Error("server accept err:", err)
			break
		}
		logger.Infof("server accept connection from: %s", connection.RemoteAddr())
		// 3.2 协程处理连接
		waitGroup.Add(1)
		go func() {
			// 3.2.1 退出协程前需要减少信号量
			defer waitGroup.Done()
			// 3.2.2
			handler.Handle(ctx, connection)
		}()
	}
	// 4. 循环退出时等待所有协程执行结束
	waitGroup.Wait()
}


