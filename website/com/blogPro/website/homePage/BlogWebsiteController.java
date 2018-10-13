package com.blogPro.website.homePage;

import java.util.ArrayList;
import java.util.List;

import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.json.JFinalJson;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.ehcache.CacheInterceptor;
import com.jfinal.plugin.ehcache.CacheKit;
import com.jfinal.plugin.ehcache.CacheName;
import com.jfinal.plugin.redis.Cache;

import cn.jbolt.common.model.Click;
import cn.jbolt.common.model.Comment;
import cn.jbolt.common.model.Noluser;
import cn.jbolt.common.model.UserCollect;
import cn.jbolt.common.model.Article;
import cn.jbolt.common.model.UserComment;
import cn.jbolt.common.model.UserFocus;


public class BlogWebsiteController extends Controller {

	/*******************首页**********************/
	public void index() {
		islog();
		render("index.html");
	}
	
	/*******************跳转文章页面**********************/
	public void art1() {
		islog();
		setAttr("mystr",getPara("title_id"));
		render("art1.html");
	}
	
	/*******************判断是否登录**********************/
	public void islog() {
		if(getSessionAttr("noluser.nolname")==null) {
			setAttr("state", false);
			setAttr("userID", 0);
		}else {
			Noluser noluser = Noluser.dao.findFirst("select * from noluser where nolname = '"+getSessionAttr("noluser.nolname")+"'");
			setAttr("state", true);
			setAttr("icon", noluser.getIcon());
			setAttr("userID", noluser.getId());
		}
	}

	/*******************退出登录方法**********************/
	public void logout() { 
		setSessionAttr("noluser.nolname", null);
		index();
	}
	
	/*******************文章页面**********************/
	public void art() {
		/*******************点击量的初始化与自增**********************/
		Click clicks = Click.dao.findFirstByCache("article","click"+getPara("title_id")+"","select * from click where id = "+getPara("title_id")+"");
		if(clicks == null) {                     
			new Click().set("id", getPara("title_id")).set("clickN", 1).set("comN",0).save();
		}else {
			Db.update("update click set clickN=clickN+1 where id = "+getPara("title_id")+"");
		}
		/*******************文章是否被登陆者收藏**********************/
		List<UserCollect> one = UserCollect.dao.findByCache("article","usercollect"+getPara("userID")+"?"+getPara("title_id")+"","select * from user_collect where user_id = "+getPara("userID")+" and article_id = "+getPara("title_id")+"");
		int collect,foc;
		if(one.isEmpty()) {
			collect = 0;
		}
		else {
			collect=1;
		}
		/*******************作者是否被登录者关注**********************/
		Article article = Article.dao.findFirstByCache("article","article"+getPara("title_id")+"","select * from article where id = "+getPara("title_id")+"");
		int focID = article.getUserid();
		UserFocus two = UserFocus.dao.findFirstByCache("article","userfocus"+getPara("userID")+"?"+focID+"","select * from user_focus where user_id = "+getPara("userID")+" and focus_id = "+focID+"");
		if(two == null) {
			foc=0;
		}
		else {
			foc=1;
		}
		/*******************获取作者信息，返回前台数据**********************/
		Noluser aut = Noluser.dao.findFirstByCache("article","noluser"+article.getUserid()+"","select * from noluser where id = "+article.getUserid()+"");
		renderJson("{\"artData\":"+JFinalJson.getJson().toJson(article)+",\"collect\":"+collect+",\"foc\":"+foc+",\"aut\":"+JFinalJson.getJson().toJson(aut) +"}"); 
	}
	
	/******************************首页加载更多功能*********************************/
	@Before(CacheInterceptor.class)
	@CacheName("article_list")
	public void list() {
		int b = getParaToInt("p");
		int c = b-5;
		List<Article> ifaces = Article.dao.find("select *,article.id from article left join click on article.id=click.id order by article.id desc limit "+c+",5");
		renderJson("{\"jsonData\":"+JFinalJson.getJson().toJson(ifaces)+"}"); 	
	}	
	
	/******************************提交评论功能 *********************************/
	

	public void com_submit() {
		Noluser aut = Noluser.dao.findFirstByCache("article","noluser"+getPara("userID")+"","select * from noluser where id = "+getPara("userID")+"");
		Comment com = new Comment().set("comc",getPara("comc")).set("comt",getPara("comt")).set("comac", getPara("comac"))
		.set("comGN",0).set("comBN",0).set("comp", aut.getNolname());
		com.save();
		List<Comment> comment = CacheKit.get("article","comment"+getPara("title_id")+"");
		comment.add(com);
		Db.update("update click set comN="+ getPara("comN")+" where id = "+getPara("title_id")+"");
		CacheKit.put("article","comment"+getPara("title_id")+"",comment);
		renderJson(com.getId()); 
	}
	
	/******************************获取评论列表功能********************************/
	public void com_list() {
		List<Comment> coms = Comment.dao.findByCache("article","comment"+getPara("title_id")+"","select * from comment where comac = "+getPara("title_id") +"");
		setAttr("comN",coms.size());
		renderJson("{\"comData\":"+JFinalJson.getJson().toJson(coms)+"}");
	}
	
	/*******************************赞评论功能***************************************/
	public void butGN() {
		int id = getParaToInt("id");
		if(getParaToInt("userID")!=0) {
			UserComment uc = UserComment.dao.findFirst("select * from user_comment where user_id = "+getPara("userID")+" and comment_id = "+getPara("id") +"");
			if(uc == null) {
				Comment comment = Comment.dao.findById(id);
				int comGN = comment.getComGN();
				comGN =comGN + 1;
				comment.setComGN(comGN).update();
				new UserComment().set("user_id", getPara("userID")).set("comment_id", id).set("yes",1).save();
				renderJson("{\"comGN\":"+comGN+"}");
			}	
		}else {
			renderJson();
		}
	}
	
	/*******************************踩评论功能***************************************/
	public void butBN() {
		int id = getParaToInt("id");
		if(getParaToInt("userID")!=0) {
			UserComment uc = UserComment.dao.findFirst("select * from user_comment where user_id = "+getPara("userID")+" and comment_id =  "+getPara("id") +"");
			if(uc == null) {
			Comment comment = Comment.dao.findById(id);
			int comBN = comment.getComBN();
			comBN =comBN + 1;
			comment.setComBN(comBN).update();
			new UserComment().set("user_id", getPara("userID")).set("comment_id", id).set("yes",0).save();
			renderJson("{\"comBN\":"+comBN+"}");
			}
		}
		else {
			renderJson();
		}
	}
	
	/*******************************返回前台此时评论区的状态*************************************/
	public void uandc() {
		UserComment uc = UserComment.dao.findFirst("select * from user_comment where user_id = "+getPara("userID")+" and comment_id =  "+getPara("id") +"");
		List<UserComment> ucs = UserComment.dao.find("select * from user_comment where Yes=1 and comment_id =  "+getPara("id") +"");		
		List<UserComment> ucss = UserComment.dao.find("select * from user_comment where Yes=0 and comment_id =  "+getPara("id") +"");
		if(uc == null) {
			renderJson("{\"comO\":1,\"numgn\":"+ucs.size()+",\"numbn\":"+ucss.size()+"}");
		}else {
			renderJson("{\"comO\":0,\"Yes\":"+uc.getYes()+",\"numgn\":"+ucs.size()+",\"numbn\":"+ucss.size()+"}");
		}
	}
	
	public void hotwen() {
		List<Article> ifaces = Article.dao.find("select  article.id,title,clickN,article.c_url from article,click where article.id = click.id order by click.clickN desc limit 4");
		System.out.println(ifaces);
		renderJson("{\"hotwenData\":"+JFinalJson.getJson().toJson(ifaces)+"}");
	}
	
	public void setfollow() {
		if(getParaToInt("userID")!=0) {
			List<UserFocus> one = UserFocus.dao.find("select * from user_focus where user_id ="+getPara("userID")+" and focus_id="+getPara("focusID")+"");
			if(one.isEmpty()) {
				new UserFocus().set("user_id", getPara("userID")).set("focus_id", getPara("focusID")).save();
				renderJson(1);
			}else {
				new UserFocus().deleteById(one.get(0).getId());
				renderJson(0);
			}
		}else {
			renderJson();
		}

	}
	
	public void setlike() {
		if(getParaToInt("userID")!=0) {
			List<UserCollect> one = UserCollect.dao.find("select * from user_collect where user_id ="+getPara("userID")+" and article_id="+getPara("articleID")+"");
			if(one.isEmpty()) {
				new UserCollect().set("user_id", getPara("userID")).set("article_id", getPara("articleID")).save();
				renderJson(1);
			}else {
				new UserCollect().deleteById(one.get(0).getId());
				renderJson(0);
			}
		}else {
			renderJson();
		}
	}
	
	public void splits() {
		List<Article> alist = Article.dao.find("SELECT title,id,c_url_img FROM article  ORDER BY  RAND() LIMIT 5");
		System.out.println("{\"sptData\":"+JFinalJson.getJson().toJson(alist)+"}");
		renderJson("{\"sptData\":"+JFinalJson.getJson().toJson(alist)+"}");
		//Math.ceil(Math.random()*num.get(0));
		//Math.ceil(Math.random());
	}
}
