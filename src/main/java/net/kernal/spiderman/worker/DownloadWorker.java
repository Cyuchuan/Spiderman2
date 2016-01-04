package net.kernal.spiderman.worker;

import net.kernal.spiderman.K;
import net.kernal.spiderman.Spiderman.Counter;
import net.kernal.spiderman.conf.Conf;
import net.kernal.spiderman.downloader.Downloader;
import net.kernal.spiderman.task.DownloadTask;
import net.kernal.spiderman.task.DuplicateCheckTask;
import net.kernal.spiderman.task.ParseTask;

/**
 * 负责下载的蜘蛛工人
 * @author 赖伟威 l.weiwei@163.com 2015-12-30
 *
 */
public class DownloadWorker extends Worker {

	private DownloadTask task;
	
	public DownloadWorker(DownloadTask task, Conf conf, Counter counter) {
		super(conf, counter);
		this.task = task;
	}
	
	public void run() {
		// 从任务包里拿到请求对象
		final Downloader.Request request = this.task.getRequest();
		// 将请求丢给下载器进行下载
		final Downloader.Response response = this.conf.getDownloader().download(request);
		if (response == null) {
			return;
		}
		// 处理重定向
		final int statusCode = response.getStatusCode();
		final String location = response.getLocation();
		if (K.isNotBlank(location) && K.isIn(statusCode, 301, 302)) {
			// 将新任务放入队列
			final DownloadTask dTask = new DownloadTask(new Downloader.Request(location), 500);
			final DuplicateCheckTask task = new DuplicateCheckTask(dTask);
			super.putTheNewTaskToDuplicateCheckQueue(task);
			return ;
		}
		if (response.getBody() == null || response.getBody().length == 0) {
			return;
		}
		// 处理响应体文本编码问题
		String charsetName = K.getCharsetName(response.getCharset());
		if (K.isBlank(charsetName)) {
			charsetName = getCharsetFromBodyStr(response.getBodyStr());
		}
		if (K.isNotBlank(charsetName)) {
			response.setCharset(charsetName);
		}
		// 获取响应体文本内容
		final String bodyStr = K.byteToString(response.getBody(), charsetName); 
		// 若内容为空，结束任务
		if (K.isBlank(bodyStr)) {
			return;
		}
		response.setBodyStr(bodyStr);
		// 下载计数＋1
		if (task.isPrimary()) {
			this.counter.primaryDownloadPlus();
		} else {
			this.counter.secondaryDownloadPlus();
		}
		
		// 报告下载事件
		this.conf.getReportings().reportDownload(response);
				
		// 将下载好的response对象放入解析队列
		final ParseTask newTask = new ParseTask(response, 500);
		super.putTheNewTaskToParseQueue(newTask);
	}

	private String getCharsetFromBodyStr(final String bodyStr) {
		if (K.isBlank(bodyStr)) {
			return null;
		}
		
		String html = bodyStr.trim().toLowerCase();
		String s1 = K.findOneByRegex(html, "(?=<meta ).*charset=.[^/]*");
		if (K.isBlank(s1)) {
			return null;
		}
		
		String s2 = K.findOneByRegex(s1, "(?=charset\\=).[^;/\"']*");
		if (K.isBlank(s2)) {
			return null;
		}
		String charsetName = s2.replace("charset=", "");
		return K.getCharsetName(charsetName);
	}
	
}