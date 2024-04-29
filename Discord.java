import com.mrpowergamerbr.temmiewebhook.*;
import com.mrpowergamerbr.temmiewebhook.embed.*;
import java.util.regex.*;
import java.util.*;

public class Discord
{
    private String color;
    private String footer;
    private String footerIcon;
    
    public Discord(final String color, final String footer, final String footerIcon) {
        this.color = color;
        this.footer = footer;
        this.footerIcon = footerIcon;
    }
    
    public void webHookMessage(final String username, final String hook, final String link, final String pic, String caption, final String profilePic, final boolean isVideo, final String video) {
        if (caption.equals("")) {
            caption = "No caption";
        }
        else {
            caption = this.clickableTags(caption);
        }
        final TemmieWebhook temmie = new TemmieWebhook(hook);
        final ImageEmbed ie = new ImageEmbed();
        caption = this.convertWWWLinks(caption);
        final FieldEmbed captionEmbed = new FieldEmbed();
        captionEmbed.setName("Caption");
        captionEmbed.setValue(caption);
        ie.setUrl(pic);
        final DiscordEmbed de = DiscordEmbed.builder().author(AuthorEmbed.builder().name(username).icon_url(profilePic).url("https://instagram.com/" + username).build()).title(String.valueOf(username) + " - Post Link").field(captionEmbed).url(link).image(ie).footer(FooterEmbed.builder().text(this.footer).icon_url(this.footerIcon).build()).build();
        if (isVideo) {
            final List<FieldEmbed> eb = new ArrayList<FieldEmbed>();
            final FieldEmbed checkVideo = new FieldEmbed();
            checkVideo.setName("Is Video?");
            checkVideo.setValue(new StringBuilder().append(isVideo).toString());
            eb.add(checkVideo);
            eb.add(captionEmbed);
            de.setFields(eb);
        }
        de.setColor(Integer.parseInt(this.color, 16));
        final DiscordMessage dm = DiscordMessage.builder().username("insta-post-monitor").content("").avatarUrl("https://diylogodesigns.com/wp-content/uploads/2016/05/instagram-Logo-PNG-Transparent-Background-download-768x768.png").embeds(Arrays.asList(de)).build();
        temmie.sendMessage(dm);
        if (isVideo) {
            final DiscordMessage dm2 = DiscordMessage.builder().username("insta-post-monitor").content(video).avatarUrl("https://diylogodesigns.com/wp-content/uploads/2016/05/instagram-Logo-PNG-Transparent-Background-download-768x768.png").build();
            temmie.sendMessage(dm2);
        }
    }
    
    public void shortook(final String hook, String link, final String avatar, final String title, final String account) {
        final TemmieWebhook temmie = new TemmieWebhook(hook);
        final ThumbnailEmbed te = new ThumbnailEmbed();
        link = this.convertWWWLinks(link);
        te.setUrl(avatar);
        final DiscordEmbed de = DiscordEmbed.builder().title(title).url("https://instagram.com/" + account).description(link).thumbnail(te).footer(FooterEmbed.builder().text(this.footer).icon_url(this.footerIcon).build()).build();
        de.setColor(Integer.parseInt(this.color, 16));
        final DiscordMessage dm = DiscordMessage.builder().username("insta-post-monitor").avatarUrl("https://diylogodesigns.com/wp-content/uploads/2016/05/instagram-Logo-PNG-Transparent-Background-download-768x768.png").embeds(Arrays.asList(de)).build();
        temmie.sendMessage(dm);
    }
    
    public void multiPhoto(final String hook, final String pic, final String title, final String link) {
        final TemmieWebhook temmie = new TemmieWebhook(hook);
        final ImageEmbed ie = new ImageEmbed();
        ie.setUrl(pic);
        final DiscordEmbed de = DiscordEmbed.builder().title(title).url(link).image(ie).footer(FooterEmbed.builder().text(this.footer).icon_url(this.footerIcon).build()).build();
        de.setColor(Integer.parseInt(this.color, 16));
        final DiscordMessage dm = DiscordMessage.builder().username("insta-post-monitor").avatarUrl("https://diylogodesigns.com/wp-content/uploads/2016/05/instagram-Logo-PNG-Transparent-Background-download-768x768.png").embeds(Arrays.asList(de)).build();
        temmie.sendMessage(dm);
    }
    
    public String convertWWWLinks(String desc) {
        final int i = desc.indexOf("www.");
        if (i != -1 && !desc.substring(i - 1, i).equals("/") && !desc.substring(i - 1, i).equals("\\")) {
            desc = String.valueOf(desc.substring(0, i)) + "https://" + desc.substring(i);
        }
        return desc;
    }
    
    public String clickableTags(String desc) {
        List<String> allMatches = new ArrayList<String>();
        Matcher m = Pattern.compile("(?<=@)[\\w-]+").matcher(desc);
        while (m.find()) {
            allMatches.add(m.group());
        }
        for (final String match : allMatches) {
            desc = desc.replace("@" + match, "[@" + match + "](https://www.instagram.com/" + match + ")");
        }
        allMatches = new ArrayList<String>();
        m = Pattern.compile("#(\\w*[0-9a-zA-Z]+\\w*[0-9a-zA-Z])").matcher(desc);
        while (m.find()) {
            allMatches.add(m.group());
        }
        for (final String match : allMatches) {
            desc = desc.replace(match, "[" + match + "](https://www.instagram.com/explore/tags/" + match.replace("#", "") + ")");
        }
        return desc;
    }
}