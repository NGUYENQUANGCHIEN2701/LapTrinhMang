package com.tankzors.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.tankzors.client.assets.AssetRepository;
import com.tankzors.client.audio.AudioManager;
import com.tankzors.client.shop.PlayerShopState;
import com.tankzors.client.shop.ShopItem;
import java.util.ArrayList;
import java.util.List;

/** Builds game.gui.Shop: full-screen catalogue and a separate scrolling help page. */
public class ShopOverlay {
    public interface ShopListener {
        void onShopClosed();
        void onShopStateChanged();
        default void onShopVisibilityChanged(boolean visible) {}
        default void onItemBought(ShopItem item) {}
        default void onItemSold(ShopItem item) {}
        default int missingTankHp() { return 0; }
        default boolean repairTank() { return false; }
        default int missingAlliedHp() { return 0; }
        default boolean repairAllies() { return false; }
    }
    private static final Color BACKGROUND = new Color(45/255f,11/255f,9/255f,1);
    private static final Color LOCKED = new Color(1,0,0,136/255f);
    private static final Color DISABLED = new Color(1,1,1,136/255f);
    private static final Color PINK = new Color(1,127/255f,127/255f,1);
    private static final int[] DIGIT_X = {0,72,111,179,247,319,387,460,529,601,673,776,842,914,987,1047};
    private static final int[] DIGIT_W = {72,39,68,68,72,68,73,69,72,72,103,66,72,73,60,37};
    private final AssetRepository assets;
    private final AudioManager audio;
    private final PlayerShopState state;
    private final ShopListener listener;
    private final List<ShopCatalog.Row> rows = ShopCatalog.rows();
    private final List<ClickArea> clicks = new ArrayList<>();
    private final GlyphLayout layout = new GlyphLayout();
    private boolean visible, help, wallet;
    private int selected;
    private float scroll, helpScroll, maxScroll, maxHelpScroll;
    private float scale = 1, rowH = 16, width, height, maxNameWidth;
    private float winX, winY, winW, winH;
    private BitmapFont font;
    private static final class ClickArea {
        final Rectangle bounds; final Runnable action;
        ClickArea(float x,float y,float w,float h,Runnable action) {
            bounds = new Rectangle(x,y,w,h); this.action = action;
        }
    }
    public ShopOverlay(AssetRepository assets, AudioManager audio, PlayerShopState state, ShopListener listener) {
        this.assets=assets; this.audio=audio; this.state=state; this.listener=listener;
    }
    public boolean isVisible() { return visible; }
    public void show() {
        if (visible) return;
        visible=true; help=false; wallet=false; selected=0; scroll=0; helpScroll=0;
        state.beginShopVisit();
        if (audio != null) { audio.playUiWindow(); audio.playBriefingMusic(); }
        if (listener != null) listener.onShopVisibilityChanged(true);
    }
    public void hide() {
        if (!visible) return;
        visible=false; clicks.clear(); state.endShopVisit();
        if (audio != null) { audio.playUiWindow(); audio.stopMusic(); }
        if (listener != null) { listener.onShopClosed(); listener.onShopVisibilityChanged(false); }
    }
    public void toggle() { if (visible) hide(); else show(); }
    public boolean handleTouchDown(float x,float y) {
        if (!visible) return false;
        for (ClickArea click:clicks) if (click.bounds.contains(x,y)) {
            if (audio != null) audio.playUiClick();
            click.action.run(); return true;
        }
        return true;
    }
    public boolean handleScrolled(float amount) {
        if (!visible) return false;
        if (help || wallet) helpScroll=clamp(helpScroll+amount*rowH,maxHelpScroll);
        else scroll=clamp(scroll+amount*rowH,maxScroll);
        return true;
    }
    public boolean handleKeyDown(int key) {
        if (!visible) { if(key==Input.Keys.B) {show();return true;} return false; }
        if (help || wallet) {
            if(key==Input.Keys.ESCAPE || key==Input.Keys.ENTER || key==Input.Keys.SPACE || key==Input.Keys.B) {
                help=false; wallet=false; helpScroll=0;
            } else if(key==Input.Keys.DOWN || key==Input.Keys.S) helpScroll=clamp(helpScroll+rowH,maxHelpScroll);
            else if(key==Input.Keys.UP || key==Input.Keys.W) helpScroll=clamp(helpScroll-rowH,maxHelpScroll);
            return true;
        }
        if(key==Input.Keys.ESCAPE || key==Input.Keys.B) { hide(); return true; }
        if(key==Input.Keys.DOWN || key==Input.Keys.S || key==Input.Keys.UP || key==Input.Keys.W) {
            int step=key==Input.Keys.DOWN || key==Input.Keys.S ? 1:-1;
            do { selected=(selected+step+rows.size())%rows.size(); } while(!rows.get(selected).selectable());
            scroll=clamp(selected*rowH-height/2,maxScroll);
        } else if(key==Input.Keys.RIGHT) change(rows.get(selected),true);
        else if(key==Input.Keys.LEFT) change(rows.get(selected),false);
        else if(key==Input.Keys.ENTER || key==Input.Keys.SPACE) describe(rows.get(selected));
        return true;
    }
    private static float clamp(float value,float max) { return Math.max(0,Math.min(max,value)); }
    private void changed() { if(listener!=null) listener.onShopStateChanged(); }
    private void change(ShopCatalog.Row row,boolean buy) {
        if(row.kind==ShopCatalog.Kind.ITEM) {
            if(!buy && !sellEnabled(row)) return;
            if(buy ? state.buy(row.item):state.sell(row.item)) {
                if(listener!=null) { if(buy) listener.onItemBought(row.item); else listener.onItemSold(row.item); }
                changed();
            }
        } else if(buy && row.kind==ShopCatalog.Kind.RESEARCH) {
            if(state.upgradeResearch(row.tech)) changed();
        } else if(buy && listener!=null && row.kind==ShopCatalog.Kind.REPAIR_TANK) {
            if(listener.repairTank()) changed();
        } else if(buy && listener!=null && row.kind==ShopCatalog.Kind.REPAIR_ALLIES) {
            if(listener.repairAllies()) changed();
        }
    }
    private void describe(ShopCatalog.Row row) {
        if(row.kind==ShopCatalog.Kind.CONTINUE) {hide();return;}
        if(row.selectable()) {help=true;helpScroll=0;}
    }
    private boolean sellEnabled(ShopCatalog.Row row) {
        return row.item!=null && row.item!=ShopItem.ARMOR && row.item!=ShopItem.ARMOR2
                && row.item!=ShopItem.RADAR && row.item!=ShopItem.EX_CHASSIS && row.item!=ShopItem.ADD_LIFE;
    }
    private int count(ShopCatalog.Row row) {
        if(row.item!=null) return state.getItemCount(row.item.getId());
        if(row.tech!=null) return state.getResearchLevel(row.tech.getId());
        if(listener==null) return 0;
        return row.kind==ShopCatalog.Kind.REPAIR_TANK?listener.missingTankHp():listener.missingAlliedHp();
    }
    private int price(ShopCatalog.Row row) {
        if(row.item!=null) return state.getEffectivePrice(row.item);
        if(row.tech!=null) return row.tech.getPriceForLevel(count(row));
        if(row.kind==ShopCatalog.Kind.REPAIR_TANK) return state.getRepairTankCost(count(row));
        return count(row)*(state.getResearchLevel("armorResearch")>=3?2:5);
    }
    private boolean buyEnabled(ShopCatalog.Row row) {
        if (row.tech != null) return count(row) < row.tech.getMaxLevel();
        if (row.item == ShopItem.ARMOR || row.item == ShopItem.ARMOR2)
            return count(row) < state.getEffectiveLimit(row.item);
        if (row.item == ShopItem.RADAR || row.item == ShopItem.EX_CHASSIS) return count(row) < 1;
        if (row.kind == ShopCatalog.Kind.REPAIR_TANK || row.kind == ShopCatalog.Kind.REPAIR_ALLIES)
            return count(row) > 0;
        return true;
    }
    private boolean canBuy(ShopCatalog.Row row) {
        if(row.item!=null) return state.canBuy(row.item);
        if(row.tech!=null) return state.canUpgradeResearch(row.tech);
        return listener!=null && count(row)>0 && state.getCoins()>=price(row);
    }
    public void render(SpriteBatch batch,ShapeRenderer shapes,BitmapFont ignored) {
        if(!visible) return;
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();
        font = assets.getShopFont();

        // 1. Kích thước cửa sổ popup modal chiếm 3/4 màn hình chơi
        winW = Math.round(screenW * 0.75f);
        winH = Math.round(screenH * 0.75f);
        winX = Math.round((screenW - winW) / 2f);
        winY = Math.round((screenH - winH) / 2f);

        width = winW;
        height = winH;

        maxNameWidth = 102;
        // Scale tỉ lệ thuận với kích thước 3/4 màn hình để giao diện to rõ, sắc nét
        scale = Math.max(1f, Math.min(winW / 260f, winH / 180f));
        rowH = 16 * scale;
        maxScroll = Math.max(0, rows.size() * rowH - height);
        scroll = clamp(scroll, maxScroll);
        clicks.clear();

        // 2. Phủ mờ màn hình nền phía sau (cho phép nhìn thấy chiến trường và xe tăng)
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.75f);
        shapes.rect(0, 0, screenW, screenH);

        // Nền cửa sổ Shop modal
        shapes.setColor(BACKGROUND);
        shapes.rect(winX, winY, winW, winH);

        // Viền ngoài màu vàng đồng phong cách quân sự như UI cũ
        shapes.setColor(0.85f, 0.65f, 0.20f, 0.90f);
        shapes.rect(winX, winY, winW, 2);
        shapes.rect(winX, winY + winH - 2, winW, 2);
        shapes.rect(winX, winY, 2, winH);
        shapes.rect(winX + winW - 2, winY, 2, winH);
        shapes.end();

        // Click ra ngoài cửa sổ để đóng Shop
        clicks.add(new ClickArea(0, 0, screenW, winY, this::hide));
        clicks.add(new ClickArea(0, winY + winH, screenW, Math.max(0, screenH - (winY + winH)), this::hide));
        clicks.add(new ClickArea(0, winY, winX, winH, this::hide));
        clicks.add(new ClickArea(winX + winW, winY, Math.max(0, screenW - (winX + winW)), winH, this::hide));

        batch.begin();
        batch.setColor(Color.WHITE);
        image(batch, assets.getShopBottom(), 0, 0, 16, 97, winX, winY + height - 97 * scale, width, 97 * scale);
        if (help || wallet) {
            renderHelp(batch);
        } else {
            for (int i = 0; i < rows.size(); i++) {
                float y = winY + i * rowH - scroll;
                if (y + rowH <= winY || y + rowH > winY + height + 2) continue;
                drawRow(batch, rows.get(i), i, y);
            }
            renderTitle(batch, shapes);
        }
        batch.setColor(Color.WHITE);
        batch.end();

        // Thanh cuộn thanh mảnh bên mép phải cửa sổ
        float maximum = help || wallet ? maxHelpScroll : maxScroll;
        float offset = help || wallet ? helpScroll : scroll;
        if (maximum > 0) {
            float top = help || wallet ? winY : (winY + (scroll < rowH ? rowH * 1.3f : rowH));
            float track = (winY + height) - top;
            float thumb = Math.max(16f, track * track / (track + maximum));
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(Color.BLACK);
            shapes.rect(winX + width - 4 * scale, top, 4 * scale, track);
            shapes.setColor(140 / 255f, 20 / 255f, 7 / 255f, 1);
            shapes.rect(winX + width - 3 * scale, top + (track - thumb) * offset / maximum, 2 * scale, thumb);
            shapes.end();
        }
    }
    private void drawRow(SpriteBatch batch,ShopCatalog.Row row,int index,float y) {
        if(row.kind==ShopCatalog.Kind.SPACE) return;
        if(row.kind==ShopCatalog.Kind.HEADER) {
            batch.setColor(1,.4f,.4f,.8f);titleLine(batch,y,scale);titleLine(batch,y,scale);
            batch.setColor(Color.WHITE);
            batch.draw(assets.getIcon(row.icon),winX+5*scale,y-scale,20*scale,20*scale);
            text(batch,row.title,winX+30*scale,y,Color.WHITE,scale);return;
        }
        boolean current=index==selected;
        if(current) selection(batch,y);
        if(row.kind==ShopCatalog.Kind.CONTINUE) {
            text(batch,row.title,winX+5*scale,y,current?Color.WHITE:Color.RED,scale);
            area(winX,y,width,rowH,this::hide);return;
        }
        boolean buy=canBuy(row);
        Color nameColor=current?(buy?Color.WHITE:PINK):(buy?Color.RED:LOCKED);
        text(batch,row.title,winX+5*scale,y,nameColor,scale);
        float nw=8*scale;
        float priceW=34*scale;
        float x=winX+width-priceW*2-10*scale-nw*8;
        float maxText=maxNameWidth*scale;
        if(x<winX+maxText+10*scale) x=(float)Math.floor(winX+maxText+10*scale+(width-maxText-10*scale-priceW)/2-(nw*11+2*scale)/2);
        final int chosen=index;
        area(winX,y,Math.max(0,x-winX-nw),rowH,()->selected=chosen);
        symbol(batch,'?',x,y+3*scale,Color.WHITE,scale);
        area(x-nw,y,nw*3,rowH,()->{selected=chosen;describe(row);});
        x+=nw*3;
        if(sellEnabled(row)) {
            boolean sell=state.canSell(row.item);
            symbol(batch,'-',x,y+3*scale,sell?Color.WHITE:DISABLED,scale);
            if(sell) area(x-nw,y,nw*2,rowH,()->{selected=chosen;change(row,false);});
        }
        x+=nw*2+2*scale;
        String value=row.tech!=null && "extraInterest".equals(row.tech.getId())?(5+count(row)*5)+"%":String.valueOf(count(row));
        digits(batch,value,x+nw-digitsWidth(value,scale)/2,y+3*scale,buy?Color.WHITE:DISABLED,scale);
        x+=nw*3;
        if (buyEnabled(row)) symbol(batch,'+',x,y+3*scale,buy?Color.WHITE:DISABLED,scale);
        if(buy) area(x-nw,y,nw*3,rowH,()->{selected=chosen;change(row,true);});
        int cost=price(row);
        if(cost>0) {
            float iconSize=14*scale;
            float iconX=winX+width-10*scale-iconSize;
            float valueX=iconX-scale-textWidth(String.valueOf(cost),scale);
            text(batch,String.valueOf(cost),valueX,y,current?(buy?Color.WHITE:DISABLED):(buy?Color.RED:LOCKED),scale);
            batch.setColor(Color.WHITE);
            image(batch,row.tech==null?assets.getStatMoney():assets.getStatStar(),0,0,
                    row.tech==null?assets.getStatMoney().getWidth():assets.getStatStar().getWidth(),
                    row.tech==null?assets.getStatMoney().getHeight():assets.getStatStar().getHeight(),iconX+3*scale,y+2*scale,iconSize,iconSize);
        }
    }
    private void renderTitle(SpriteBatch batch,ShapeRenderer shapes) {
        float headerScale=scroll<rowH?scale*1.3f:scale;
        float headerH=16*headerScale;
        batch.end();shapes.begin(ShapeRenderer.ShapeType.Filled);shapes.setColor(BACKGROUND);shapes.rect(winX,winY,width,headerH);shapes.end();batch.begin();
        batch.setColor(1,144/255f,144/255f,1);
        titleLine(batch,winY,headerScale);titleLine(batch,winY,headerScale);titleLine(batch,winY,headerScale);
        batch.setColor(Color.WHITE);image(batch,assets.getShopFade(),0,0,16,16,winX,winY+headerH-1,width,headerH);
        symbol(batch,'<',winX+10*headerScale,winY+3*headerScale,Color.WHITE,headerScale);
        float iconSize=14*headerScale;
        float moneyW=textWidth(String.valueOf(state.getCoins()),headerScale);
        float starsW=textWidth(String.valueOf(state.getStars()),headerScale);
        font.getData().setScale(headerScale/4);
        float space=font.getData().spaceXadvance;
        float all=22*headerScale+moneyW+starsW+3*space;
        float x=winX+(width-all)/2;
        batch.setColor(Color.WHITE);
        image(batch,assets.getStatMoney(),0,0,assets.getStatMoney().getWidth(),assets.getStatMoney().getHeight(),x,winY+2*headerScale,iconSize,iconSize);
        text(batch,String.valueOf(state.getCoins()),x+iconSize+1,winY,Color.WHITE,headerScale);
        float starX=x+iconSize+1+moneyW+2*space;
        batch.setColor(Color.WHITE);image(batch,assets.getStatStar(),0,0,assets.getStatStar().getWidth(),assets.getStatStar().getHeight(),starX,winY+2*headerScale,iconSize,iconSize);
        text(batch,String.valueOf(state.getStars()),starX+iconSize+1,winY,Color.WHITE,headerScale);
        float plus=winX+(width+all)/2+8*headerScale;
        symbol(batch,'+',plus,winY+3*headerScale,new Color(1,1,.72f,1),headerScale);
        // Header input takes priority over rows scrolling underneath it.
        final float limit=winY+headerH;
        clicks.removeIf(c->c.bounds.y>=winY && c.bounds.y<limit);
        clicks.add(0,new ClickArea(winX,winY,28*headerScale,headerH,this::hide));
        clicks.add(0,new ClickArea(x,winY,Math.max(all,plus-x+12*headerScale),headerH,()->{wallet=true;helpScroll=0;}));
    }
    private void renderHelp(SpriteBatch batch) {
        ShopCatalog.Row row=rows.get(selected);
        String description;
        if(wallet) description="Xu: "+state.getCoins()+"\n\nSao: "+state.getStars();
        else if(row.item!=null) description=row.title+"\n\nGiá: "+price(row)+" Xu\n\n"+state.getItemDescription(row.item)+"\n\n"+state.getInventoryUsage(row.item);
        else if(row.tech!=null) description=row.title+"\n\n"+row.tech.getDescription();
        else description=row.title+"\n\nSửa toàn bộ HP còn thiếu. Giá: "+price(row)+" Xu.";
        font.getData().setScale(scale/4);
        float lineWidth=width*.75f;
        layout.setText(font,description,Color.RED,lineWidth,com.badlogic.gdx.utils.Align.left,true);
        float contentH=layout.height+rowH*4;
        maxHelpScroll=Math.max(0,contentH-height);helpScroll=clamp(helpScroll,maxHelpScroll);
        float top=winY+(contentH<height?(height-contentH)/2:0)+rowH-helpScroll;
        float backY=top+layout.height+rowH;
        font.setColor(Color.BLACK);font.draw(batch,description,winX+width/2-lineWidth/2-.5f*scale,top+.5f*scale,lineWidth,com.badlogic.gdx.utils.Align.left,true);
        font.setColor(Color.RED);font.draw(batch,description,winX+width/2-lineWidth/2,top,lineWidth,com.badlogic.gdx.utils.Align.left,true);
        batch.setColor(Color.WHITE);image(batch,assets.getBillets(),360,0,126,16,winX+width/2-63*scale,backY,126*scale,rowH);
        text(batch,"Quay lại",winX+width/2-textWidth("Quay lại",scale)/2,backY,Color.WHITE,scale);
        area(winX+width/2-63*scale,backY,126*scale,rowH,()->{help=false;wallet=false;helpScroll=0;});
    }
    private void area(float x,float y,float w,float h,Runnable action) {
        float limitTop = winY + 16 * (scroll < rowH ? scale * 1.3f : scale);
        float top=Math.max(limitTop,y),bottom=Math.min(winY+height,y+h);
        if(w>0 && bottom>top) clicks.add(new ClickArea(x,top,w,bottom-top,action));
    }
    private float textWidth(String text,float s) {
        font.getData().setScale(s/4);layout.setText(font,text);return layout.width;
    }
    private void text(SpriteBatch batch,String text,float x,float y,Color color,float s) {
        font.getData().setScale(s/4);
        float top=y+(16*s-font.getCapHeight())/2;
        font.setColor(Color.BLACK);font.draw(batch,text,x-.5f*s,top+.5f*s);
        font.setColor(color);font.draw(batch,text,x,top);
    }
    private void image(SpriteBatch batch,Texture texture,int sx,int sy,int sw,int sh,float x,float y,float w,float h) {
        if(w<=0 || h<=0) return;
        TextureRegion region=new TextureRegion(texture,sx,sy,sw,sh);region.flip(false,true);batch.draw(region,x,y,w,h);
    }
    private void titleLine(SpriteBatch batch,float y,float s) {
        float first=Math.min(width,125*s);
        image(batch,assets.getBillets(),540,0,(int)(first/s),16,winX,y,first,16*s);
        image(batch,assets.getBillets(),640,0,25,16,winX+first,y,width-first,16*s);
    }
    private void selection(SpriteBatch batch,float y) {
        batch.setColor(Color.WHITE);
        float first=Math.min(width,350*scale);
        image(batch,assets.getBillets(),0,0,(int)(first/scale),16,winX,y,first,rowH);
        image(batch,assets.getBillets(),260,0,90,16,winX+first,y,width-first,rowH);
    }
    private int digit(char c) { return c>='0'&&c<='9'?c-'0':c=='%'?10:c=='?'?11:c=='-'?12:c=='+'?13:c=='<'?14:15; }
    private float digitsWidth(String text,float s) {
        float result=0;for(int i=0;i<text.length();i++) result+=(DIGIT_W[digit(text.charAt(i))]-3)*.125f*s;return result;
    }
    private void digits(SpriteBatch batch,String text,float x,float y,Color color,float s) {
        batch.setColor(color);float factor=.125f*s;
        for(int i=0;i<text.length();i++) {int id=digit(text.charAt(i));image(batch,assets.getShopDigits(),DIGIT_X[id],0,DIGIT_W[id],95,x,y,DIGIT_W[id]*factor,95*factor);x+=(DIGIT_W[id]-3)*factor;}
    }
    private void symbol(SpriteBatch batch,char c,float x,float y,Color color,float s) {
        int id=digit(c);batch.setColor(color);float factor=.125f*s;
        image(batch,assets.getShopDigits(),DIGIT_X[id],0,DIGIT_W[id],95,x+Math.max(0,73-DIGIT_W[id])*.5f*factor,y,DIGIT_W[id]*factor,95*factor);
    }
}
