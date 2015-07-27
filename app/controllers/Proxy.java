package controllers;

import play.mvc.Controller;
import play.mvc.Result;

public class Proxy extends Controller {

	public Result proxy(String url) {
		return ok ("Verwerkt: " + url).as ("text/html");
	}

}
