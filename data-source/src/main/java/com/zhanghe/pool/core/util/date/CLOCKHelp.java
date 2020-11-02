package com.zhanghe.pool.core.util.date;

/**
 * 时钟帮助器
 * @author: ZhangHe
 * @since: 2020/10/21 15:12
 */
public abstract class CLOCKHelp {

    /**
     * 已经逝去的时间
     * @param startTime 开始时间
     * @return 返回目前到开始时间的间隔 单位:毫秒
     */
    public static long elapseMillis(long startTime){
        return System.currentTimeMillis() - startTime;
    }
}
