package net.kernal.spiderman.conf;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import net.kernal.spiderman.Properties;
import net.kernal.spiderman.Spiderman;
import net.kernal.spiderman.Spiderman.Seeds;
import net.kernal.spiderman.Spiderman.Targets;
import net.kernal.spiderman.Target;
import net.kernal.spiderman.TaskManager;
import net.kernal.spiderman.downloader.DefaultDownloader;
import net.kernal.spiderman.queue.DefaultTaskQueue;
import net.kernal.spiderman.reporting.ConsoleReporting;

/**
 * 默认的配置构建器
 * @author 赖伟威 l.weiwei@163.com 2015-12-01
 *
 */
public abstract class DefaultConfBuilder implements Spiderman.Conf.Builder {

	protected Spiderman.Conf conf;
	public DefaultConfBuilder() {
		super();
		conf = new Spiderman.Conf();
		conf.setTaskQueue(new TaskManager(new DefaultTaskQueue(), new DefaultTaskQueue()))
			.setDownloader(new DefaultDownloader(conf.getProperties()))
			.addReporting(new ConsoleReporting());
	}
	/**
	 * 留给客户端程序去添加属性
	 * @param properties
	 */
	public abstract void addProperty(Properties properties);
	/**
	 * 留给客户端程序去添加种子
	 * @param seeds
	 */
	public abstract void addSeed(Seeds seeds);
	/**
	 * 留给客户端程序去添加目标
	 * @param targets
	 */
	public abstract void addTarget(Targets targets);
	
	/**
	 * 构建Spiderman.Conf对象
	 */
	public Spiderman.Conf build() {
		this.addProperty(conf.getProperties());
		final String engineName = conf.getProperties().getString("scriptEngine", "nashorn");
		final ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName(engineName);
		this.conf.setScriptEngine(scriptEngine);
		
		this.addSeed(conf.getSeeds());
		this.addTarget(conf.getTargets());
		for (Target target : conf.getTargets().all()) {
			target.configModel(target.getModel());
			target.configRules(target.getRules());
		}
		return conf;
	}

}