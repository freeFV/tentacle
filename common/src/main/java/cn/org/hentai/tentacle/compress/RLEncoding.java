package cn.org.hentai.tentacle.compress;

import cn.org.hentai.tentacle.util.ByteUtils;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by matrixy on 2018/4/11.
 */
public class RLEncoding extends BaseCompressProcessor
{
    // 压缩后的图像字节数组
    private static ByteArrayOutputStream compressedData = new ByteArrayOutputStream(1024 * 1024 * 2);

    // 以RGB作为数组下标的数组容器，用于保存颜色的出现次数，或是颜色表的下标
    private static int[] colortable = new int[1 << 24];

    // 保存己出现的颜色
    private static int[] colors = new int[1920 * 1080];

    // 最少需要N次出现才会加入到颜色表中
    private static int N = 20;

    // 保存256个出现最多次的颜色
    private static int[] mainColors = new int[512];

    // mainColors数组的有效数据下标，也指代了颜色个数
    private static int colorIndex = 0;

    public synchronized byte[] compress(int[] bitmap)
    {
        // 初始化
        compressedData.reset();

        // 查找出现次数最多的颜色，建立颜色表
        findMainColors(bitmap);

        // 颜色表/行程编码
        int rl = 1;
        int lastColor = bitmap[0] & 0xffffff;
        for (int i = 1; i < bitmap.length; i++)
        {
            int color = bitmap[i] & 0xffffff;
            if (color == lastColor && rl < 127)
            {
                rl += 1;
            }
            else
            {
                if (colortable[lastColor] == 0)
                {
                    compressedData.write(rl);
                    compressedData.write((byte) ((lastColor >> 16) & 0xff));
                    compressedData.write((byte) ((lastColor >> 8) & 0xff));
                    compressedData.write((byte) (lastColor & 0xff));
                }
                else
                {
                    compressedData.write(rl | 0x80);
                    compressedData.write(colortable[lastColor]);
                }
                rl = 1;
                lastColor = color;
            }
        }

        // 清空colortable
        for (int i = 0; i < mainColors.length; i+=2) colortable[mainColors[i + 1]] = 0;

        return compressedData.toByteArray();
    }

    // 查找次数出现最多的颜色
    public static void findMainColors(int[] bitmap)
    {
        // 重置
        colorIndex = 0;
        Arrays.fill(mainColors, 0);

        // 颜色计数
        for (int i = 0; i < bitmap.length; i++)
        {
            int color = bitmap[i] & 0xffffff;
            if (colortable[color] == 0) colors[colorIndex++] = color;
            colortable[color] += 1;
        }

        // 查找主颜色
        int minCount = 0;
        for (int i = 0; i < colorIndex; i++)
        {
            int color = colors[i];
            int count = colortable[color];

            // 将colorCounting清零
            colortable[color] = 0;

            if (count < N) continue;

            // 如果比mainColors里最小的都还要少，后面的事情也不用弄了
            if (count < minCount) continue;

            int k = 0;
            for (; k < mainColors.length; k+=2)
            {
                if (mainColors[k] < count)
                {
                    if (k < mainColors.length - 2) System.arraycopy(mainColors, k, mainColors, k + 2, mainColors.length - k - 2);

                    mainColors[k] = count;
                    mainColors[k + 1] = color;
                    break;
                }
            }
            minCount = mainColors[mainColors.length - 2];
        }

        for (int i = 0; i < mainColors.length; i+=2)
        {
            int count = mainColors[i];
            if (count == 0) continue;
            colortable[mainColors[i + 1]] = count;
        }
    }

    // 测试用：压缩后的图像数据解压
    private int[] decompress(byte[] imageData)
    {
        return null;
    }

    public static void init()
    {
        // 初始化
    }
}