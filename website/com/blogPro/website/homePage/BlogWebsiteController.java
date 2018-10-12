package com.blogPro.website.homePage;

import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.List;

import com.jfinal.core.Controller;
import com.jfinal.json.JFinalJson;
import com.jfinal.plugin.activerecord.Db;

import cn.jbolt.common.model.Click;
import cn.jbolt.common.model.Comment;
import cn.jbolt.common.model.Noluser;
import cn.jbolt.common.model.UserCollect;
import cn.jbolt.common.model.Article;
import cn.jbolt.common.model.UserComment;
import cn.jbolt.common.model.UserFocus;


public class BlogWebsiteController extends Controller {
	public void index() {
		islog();
		render("index.html");
	}
	
	public void art1() {
		islog();
		setAttr("mystr",getPara("title_id"));
		render("art1.html");
	}
//    判断是否登录
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
//	退出登录方法
	public void logout() { 
		setSessionAttr("noluser.nolname", null);
		index();
	}
	public void art() {
		List<Click> clicks = Click.dao.find("select * from click where id = "+getPara("title_id")+"");
		if(clicks.isEmpty()) {                     
			new Click().set("id", getPara("title_id")).set("clickN", 1).set("comN",0).save();
		}else {
			Db.update("update click set clickN=clickN+1 where id = "+getPara("title_id")+"");
		}
		List<UserCollect> one = UserCollect.dao.find("select * from user_collect where user_id = "+getPara("userID")+" and article_id = "+getPara("title_id")+"");
		int collect,foc;
		if(one.isEmpty()) {
			collect = 0;
		}
		else {
			collect=1;
		}
		List<Article> article = Article.dao.find("select * from article where id = "+getPara("title_id")+"");
		int focID = article.get(0).getUserid();
		List<UserFocus> two = UserFocus.dao.find("select * from user_focus where user_id = "+getPara("userID")+" and focus_id = "+focID+"");
		if(two.isEmpty()) {
			foc=0;
		}
		else {
			foc=1;
		}
		List<Noluser> aut = Noluser.dao.find("select * from noluser where id = "+article.get(0).getUserid()+"");
		renderJson("{\"artData\":"+JFinalJson.getJson().toJson(article)+",\"collect\":"+collect+",\"foc\":"+foc+",\"aut\":"+JFinalJson.getJson().toJson(aut) +"}"); 
	}
	
	public void list() {
		int b = getParaToInt("p");
		int c = b-5;
		List<Article> ifaces = Article.dao.findByCache("article_list","articles","select *,article.id from article left join click on article.id=click.id order by article.id desc limit "+c+",5");
		//出现问题，点击加载更多的时候会出现直接从缓存里取出重复的数据
		//List<Click> clicks = Click.dao.find("select article.id,clickN,comN from click,article where article.id = click.id");
		//System.out.println("{\"jsonData\":"+JFinalJson.getJson().toJson(ifaces)+",\"comData\":"+JFinalJson.getJson().toJson(clicks)+"}");
		renderJson("{\"jsonData\":"+JFinalJson.getJson().toJson(ifaces)+"}"); 	
	}	
	
	public void com_submit() {
		List<Noluser> aut = Noluser.dao.find("select nolname from noluser where id = "+getPara("userID")+"");
		new Comment().set("comc",getPara("comc")).set("comt",getPara("comt")).set("comac", getPara("comac"))
		.set("comGN",0).set("comBN",0).set("comp", aut.get(0).getNolname()).save();
		Db.update("update click set comN="+ getPara("comN")+" where id = "+getPara("title_id")+"");
		renderJson(); 
	}
	
	public void com_list() {
		List<Comment> coms = Comment.dao.find("select * from comment where comac = "+getPara("title_id") +"");
		setAttr("comN",coms.size());
		renderJson("{\"comData\":"+JFinalJson.getJson().toJson(coms)+"}");
	}
	
	public void butGN() {
		int id = getParaToInt("id");
		if(getParaToInt("userID")!=0) {
			List<UserComment> uc = UserComment.dao.find("select * from user_comment where user_id = "+getPara("userID")+" and comment_id =  "+getPara("id") +"");
			if(uc.isEmpty()) {
				Comment comment = Comment.dao.findById(id);
				int comGN = comment.getComGN();
				comGN =comGN + 1;
				comment.setComGN(comGN).update();
				new UserComment().set("user_id", getPara("userID")).set("comment_id", id).set("yes",1).save();
			}	
		}
		renderJson();
	}
	public void butBN() {
		int id = getParaToInt("id");
		if(getParaToInt("userID")!=0) {
			List<UserComment> uc = UserComment.dao.find("select * from user_comment where user_id = "+getPara("userID")+" and comment_id =  "+getPara("id") +"");
			if(uc.isEmpty()) {
			Comment comment = Comment.dao.findById(id);
			int comBN = comment.getComBN();
			comBN =comBN + 1;
			comment.setComBN(comBN).update();
			new UserComment().set("user_id", getPara("userID")).set("comment_id", id).set("yes",0).save();
			}
		}
		renderJson();
	}
	
	public void uandc() {
		List<UserComment> uc = UserComment.dao.find("select * from user_comment where user_id = "+getPara("userID")+" and comment_id =  "+getPara("id") +"");
		List<UserComment> ucs = UserComment.dao.find("select * from user_comment where Yes=1 and comment_id =  "+getPara("id") +"");		
		List<UserComment> ucss = UserComment.dao.find("select * from user_comment where Yes=0 and comment_id =  "+getPara("id") +"");
		if(uc.isEmpty()) {
			renderJson("{\"comO\":1,\"numgn\":"+ucs.size()+",\"numbn\":"+ucss.size()+"}");
		}else {
			renderJson("{\"comO\":0,\"Yes\":"+uc.get(0).getYes()+",\"numgn\":"+ucs.size()+",\"numbn\":"+ucss.size()+"}");
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
