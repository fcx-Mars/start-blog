package com.blogPro.website.message;

import java.util.List;

import com.jfinal.core.Controller;
import com.jfinal.json.JFinalJson;

import cn.jbolt.common.model.Article;
import cn.jbolt.common.model.Comment;
import cn.jbolt.common.model.Noluser;
import cn.jbolt.common.model.UserCollect;
import cn.jbolt.common.model.UserFocus;

public class MessageWebsiteController extends Controller {
	public void index() {
		if(islog()) {
			render("index.html");
		}else {
			redirect("/website/login");
		}
		
	}
	public boolean islog() {
		if(getSessionAttr("noluser.nolname")==null) {
			setAttr("state", false);
			setAttr("userID", 0);
			return false;
		}else {
			Noluser noluser = Noluser.dao.findFirst("select * from noluser where nolname = '"+getSessionAttr("noluser.nolname")+"'");
			setAttr("state", true);
			setAttr("icon", noluser.getIcon());
			setAttr("userID", noluser.getId());
			return true;
		}
	}
	public void getSendCommentJson() {
		Noluser user = Noluser.dao.findFirst("select nolname,icon from noluser where id = "+getPara("userID")+""); 
		String thename = user.getNolname();
		String icon = user.getIcon();
		List<Comment> send_comments = Comment.dao.find("select comt,comp,comc,title from comment left join article  on comment.comac=article.id where comp = \""+thename+"\"");
		renderJson("{\"sendCommentData\":"+JFinalJson.getJson().toJson(send_comments)+",\"jsonLen\":"+send_comments.size()+",\"icon\":\""+icon+"\"}");
	}
	
	public void getGetCommentJson() {
		List<Article> articles = Article.dao.find("select article.id from article where userid = "+getPara("userID")+" ");
		int len = articles.size();
		String ids = "(";
		int i ;
		for(i=0;i<len-1;i++) {
			ids += articles.get(i).getId() + ",";
		}
		ids += articles.get(len-1).getId() + ")";
		List<Comment> get_comments = Comment.dao.find("select comt,comp,comc,title,icon from comment left join article  on comment.comac=article.id left join noluser on comp = nolname where comac in "+ids+"");
		renderJson("{\"getCommentData\":"+JFinalJson.getJson().toJson(get_comments)+",\"jsonLen\":"+get_comments.size()+"}");
	}
	
	public void getCollectJson() {
		List<UserCollect> userCollects = UserCollect.dao.find("select article_id from user_collect where user_id = "+getPara("userID")+"");
		int len = userCollects.size();
		String ids = "(";
		int i ;
		for(i=0;i<len-1;i++) {
			ids += userCollects.get(i).getArticleId() + ",";
		}
		ids += userCollects.get(len-1).getArticleId() + ")";
		List<Article> get_collects =Article.dao.find("select title,c_url_img,s_content,author,clickN,comN from article left join click on article.id = click.id where article.id in "+ids+"");
		renderJson("{\"getCollectData\":"+JFinalJson.getJson().toJson(get_collects)+",\"jsonLen\":"+get_collects.size()+"}");
	}
	
	public void getFanedJson() {
		List<UserFocus> userFocus = UserFocus.dao.find("select focus_id from user_focus where user_id = "+getPara("userID")+"");
		String ids = "(";
		int i ;
		for(i=0;i<userFocus.size();i++) {
			ids += userFocus.get(i).getFocusId() + ",";
		}
		ids += userFocus.get(userFocus.size()-1).getFocusId() + ")";
		System.out.println("weishm:"+ids);
		List<Noluser> nolusers = Noluser.dao.find("select nolname,discription,id,icon from noluser where id in "+ids+"");
		renderJson("{\"getFanedData\":"+JFinalJson.getJson().toJson(nolusers)+",\"jsonLen\":"+nolusers.size()+"}");
	}
	
	public void getFaningJson() {
		List<UserFocus> userFocus = UserFocus.dao.find("select focus_id from user_focus where user_id = "+getPara("userID")+"");
		String ids = "(";
		int i ;
		for(i=0;i<userFocus.size();i++) {
			ids += userFocus.get(i).getFocusId() + ",";
		}
		ids += userFocus.get(userFocus.size()-1).getFocusId() + ")";
		List<Noluser> nolusers = Noluser.dao.find("select nolname,discription,id,icon from noluser where id not in "+ids+" order by rand() limit 5");
		renderJson("{\"getFaningData\":"+JFinalJson.getJson().toJson(nolusers)+",\"jsonLen\":"+nolusers.size()+"}");
	}
	
	public void setFan() {
		if(getParaToInt("userID")!=null) {
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
}
