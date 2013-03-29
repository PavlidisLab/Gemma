package ubic.gemma.web.spring.integration.twitter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.social.twitter.api.impl.TwitterTemplate;

@Configuration
public class TwitterConfigurationTemplate {
	@Value("${twitter.consumer-key}")
	private String consumerKey;
	
	@Value("${twitter.consumer-secret}")
	private String consumerSecret;
	
	@Value("${twitter.access-token}")
	private String accessToken;
	
	@Value("${twitter.access-token-secret}")
	private String accessTokenSecret;
	
	@Bean
	public TwitterTemplate twitterTemplate() {
		TwitterTemplate twitterOperations = new TwitterTemplate(consumerKey, consumerSecret, accessToken, accessTokenSecret);
		return twitterOperations;
	}
}
