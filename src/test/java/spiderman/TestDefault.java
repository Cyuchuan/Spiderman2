package spiderman;

import com.alibaba.fastjson.JSON;

import net.kernal.spiderman.Context;
import net.kernal.spiderman.K;
import net.kernal.spiderman.Properties;
import net.kernal.spiderman.Seed;
import net.kernal.spiderman.Spiderman;
import net.kernal.spiderman.conf.Conf;
import net.kernal.spiderman.conf.Conf.Pages;
import net.kernal.spiderman.conf.Conf.Seeds;
import net.kernal.spiderman.conf.DefaultConfBuilder;
import net.kernal.spiderman.logger.Logger;
import net.kernal.spiderman.worker.extract.HtmlCleanerExtractor;
import net.kernal.spiderman.worker.extract.TextExtractor;
import net.kernal.spiderman.worker.extract.conf.Model;
import net.kernal.spiderman.worker.extract.conf.Page;

public class TestDefault {
	
	/**
	 * 这个测试代码完全使用Java代码的方式来配置抽取规则，可以看到配置躲起来之后代码不太好看，至少是比较繁杂的。
	 * 另外一个TestXML例子就使用大部分配置通过XML文件加载，小部分用Java代码处理，看起来会好很多。
	 */
	public static void main(String[] args) throws InterruptedException {
		Conf conf = new DefaultConfBuilder() {
			public void configPages(Pages pages) {
				pages.add(new Page("最后结果") {
					public void config(UrlMatchRules rules, Models models) { 
						this.setExtractorBuilder(TextExtractor.builder());
						rules.addNegativeContainsRule("baidu"); 
					}
				});
				pages.add(new Page("百度网页搜索") {
					public void config(UrlMatchRules rules, Models models) {
						this.setExtractorBuilder(HtmlCleanerExtractor.builder());
						rules.addRegexRule("(?=http://www\\.baidu\\.com/s\\?wd\\=).[^&]*(&pn\\=\\d+)?");
						Model model = models.addModel("demo");
						model.addField("详情URL")
							.set("xpath", "//div[@id='content_left']//div[@class='result c-container ']//h3//a[@href]")
							.set("attribute", "href")
							.set("isArray", true)
							.set("isForNewTask", true);
						model.addField("分页URL")
						.set("xpath", "//div[@id='page']//a[@href]")
						.set("attr", "href")
						.set("isArray", true)
						.set("isDistinct", true)
						.set("isForNewTask", true)
						.addFilter((e, v) -> {
							final String pn = K.findOneByRegex(v, "&pn\\=\\d+");
							return K.isBlank(pn) ? v : e.getTask().getSeed().getUrl()+pn;
						});
					}
				});
			}
			public void configSeeds(Seeds seeds) {
				seeds.add(new Seed("http://www.baidu.com/s?wd="+K.urlEncode("\"蜘蛛侠\"")));
			}
			public void configParams(Properties params) {
				params.put("logger.level", Logger.LEVEL_DEBUG);
				params.put("duration", "30s");
				params.put("worker.downloader.size", 5);
//				params.put("worker.downloader.result.limit", 10);
//				params.put("worker.extractor.result.limit", 10);
				params.put("worker.extractor.size", 5);
			}
		}.build();
		
		// 启动蜘蛛侠
		Context ctx = new Context(conf, (result, count) -> {
			// handle the extract result
			System.err.println("解析到第"+count+"个:\r\n"+JSON.toJSONString(result, true));
		});
		new Spiderman(ctx).go();
	}
	
}
