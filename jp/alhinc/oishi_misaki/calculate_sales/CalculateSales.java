package jp.alhinc.oishi_misaki.calculate_sales;
/*
 * 最終更新：2017/4/25
 * 作業者：大石岬
 */


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class CalculateSales {
	public static void main(String[] args){
		if(args.length != 1){
			System.out.println("予期せぬエラーが発生しました");
			return;
		}
		//読込書出ファイル定義
		File Directry = new File(args[0]);
		File branchDefinitionFile = new File(args[0], "branch.lst");
		File commodityDefinitionFile = new File(args[0] + File.separator + "commodity.lst");
		File branchSaleOutPutFile = new File(args[0] + File.separator + "branch.out");
		File commoditySaleOutPutFile = new File(args[0] + File.separator + "commodity.out");
		HashMap<String, String> branchCodeAndNameMap = new HashMap<>();			//brc(支店コード,支店名)
		HashMap<String, String> commodityCodeAndNameMap = new HashMap<>();		//com(商品コード,商品名)
		HashMap<String, Long>branchTatalSaleMap = new HashMap<>();	//brcsum(支店コード,支店別売上)
		HashMap<String, Long> commodityTatalSaleMap  = new HashMap<>();	//comsum(商品コード,商品別売上)
		try{
			//支店定義ファイル読込
			branchCodeAndNameMap = readBranchDefinitionFile(branchDefinitionFile);
			if(branchCodeAndNameMap == null){
				return;
			}
			branchTatalSaleMap = createTatalSaleMap(branchCodeAndNameMap);

			//商品定義ファイル読込
			commodityCodeAndNameMap = readCommodityDefinitionFile(commodityDefinitionFile);
			if(commodityCodeAndNameMap == null){
				return;
			}
			commodityTatalSaleMap = createTatalSaleMap(commodityCodeAndNameMap);

			//売上ファイル検索、読込、連番処理
			 List<File> serchedNumsFile = searchFile(Directry);
			 if(serchedNumsFile == null){
				 return;
			 }
			 //売上ファイルの読込・加算
			 if(!readBranchSaleFileAndSum(serchedNumsFile, branchCodeAndNameMap, commodityCodeAndNameMap, branchTatalSaleMap, commodityTatalSaleMap)){
				return;
			}
			//ファイルの書出し
			if(!writeTotalSaleFile(branchSaleOutPutFile, branchCodeAndNameMap, branchTatalSaleMap)){
				return;
			}
			if(!writeTotalSaleFile(commoditySaleOutPutFile, commodityCodeAndNameMap, commodityTatalSaleMap)){
				return;
			}
		}catch(IOException e){
			System.out.println("予期せぬエラーが発生しました");
			return;
		}
	}

	/*
	 * 支店定義ファイルの読み込み
	 *@param File : 支店定義ファイル
	 *@return HasMap<String, String>:支店コード、支店名
	 *＠error ファイルが存在しない
	 *@error 不正フォーマット
	 *@error 予期せぬエラー
	 */
	public static HashMap<String, String> readBranchDefinitionFile(File branchDefinitionFile) throws IOException{
		BufferedReader br = null;
		HashMap<String, String> branchCodeAndName = new HashMap<>();
		try{
			if(!branchDefinitionFile.exists()){
				System.out.println("支店定義ファイルが存在しません");
				return null;
			}
			br = new BufferedReader(new FileReader(branchDefinitionFile));
			String s;
			while((s = br.readLine()) != null){
				String[] CodeAndName = s.split(",");
				if(CodeAndName.length != 2 || !CodeAndName[0].matches("\\d{3}$") || CodeAndName[1].isEmpty()){
					System.out.println("支店定義ファイルのフォーマットが不正です");
					return null ;
				}
				branchCodeAndName.put(CodeAndName[0],CodeAndName[1]);
			}
			return branchCodeAndName;
		}catch(IOException e){
			System.out.println("予期せぬエラーが発生しました");
			return null;
		}finally{
			if(br != null){
				br.close();
			}
		}
	}
	/*
	 * 支店定義/商品定義のマップを元に支店別商品別売上合計マップの作成
	 *branchCodeAndName　－＞　createBranchTatalSaleMap
	 *commodityCodeAndNameMap　－＞　commodityTatalSale
	 */
	public static HashMap<String, Long> createTatalSaleMap(HashMap<String, String> CodeAndNameMap) throws IOException{
		HashMap<String, Long>TatalSaleMap = new HashMap<>();
		for(Map.Entry<String, String> entries : CodeAndNameMap.entrySet()){
			TatalSaleMap.put(entries.getKey(),0L);
		}
		return TatalSaleMap;
	}
	/*
	 * 商品定義ファイルの読み込み
	 *@param f@param File : 商品定義ファイル
	 *@return HasMap<String, String>:商品コード、商品名
	 *＠error ファイルが存在しない
	 *@error 不正フォーマット
	 *@error 予期せぬエラー
	 */
	public static HashMap<String, String> readCommodityDefinitionFile(File commodityDefinitionFile) throws IOException{
		BufferedReader br = null;
		HashMap<String, String> commodityCodeAndName = new HashMap<>();
		try{
			if(!commodityDefinitionFile.exists()){
				System.out.println("商品定義ファイルが存在しません");
				return null;
			}
			FileReader fr = new FileReader(commodityDefinitionFile);
			br = new BufferedReader(fr);
			String s;
			while((s = br.readLine()) != null){
				String[] CodeAndName = s.split(",");
				if(CodeAndName.length != 2 || !CodeAndName[0].matches("^[0-9a-zA-Z]{8}$") || CodeAndName[1].isEmpty()){
					System.out.println("商品定義ファイルのフォーマットが不正です");
					return null;
				}
				commodityCodeAndName.put(CodeAndName[0], CodeAndName[1]);
			}
			return commodityCodeAndName;
		}catch(IOException e){
			System.out.println("予期せぬエラーが発生しました");
			return null;
		}finally{
			if(br != null){
				br.close();
			}
		}
	}
	/*
	 * 売上ファイル検索
	 * @param ディレクトリ名
	 * ＠return 検索結果のファイルList
	 */
	public static List<File> searchFile(File Directry){
		List<File> directryList = Arrays.asList(Directry.listFiles());
		String index = "^\\d{8}.rcd$";
		List<File> searchedFile = new ArrayList<>();
		List<Integer> nums = new ArrayList<>();
		for(File fl : directryList){
			String fileName = fl.getName();
			if(fileName.matches(index) && fl.isFile()){
				searchedFile.add(fl);
				nums.add(Integer.parseInt(fileName.split("\\.")[0]));
			}
		}
		Collections.sort(nums);
		int min = nums.get(0);
		int max = nums.get(nums.size() - 1);
		if(min + nums.size() != max + 1){
			System.out.println("売上ファイル名が連番になっていません");
			return null ;
		}
		return searchedFile;
	}
	/*
	 * ファイル読込・加算
	 *＠param 売上ファイルリスト
	 *@param 支店定義
	 *@param 支店別売上合計
	 *@param 商品定義
	 *@param 商品別売上合計
	 */
	public static boolean readBranchSaleFileAndSum(List<File> numsfile,HashMap<String, String> branchCodeAndNameMap, HashMap<String, String> commodityCodeAndNameMap,
			HashMap<String, Long>branchTatalSaleMap, HashMap<String, Long> commodityTatalSaleMap) throws IOException{

		BufferedReader br = null;
		for(File fl : numsfile){
			if(!fl.exists()){
				System.out.println("予期せぬエラーが発生しました");
				return false;
			}
			try{
				FileReader fr = new FileReader(fl);
				br = new BufferedReader(fr);
				String s;
				List<String> st = new ArrayList<>();
				while((s=br.readLine()) != null){
					st.add(s);
				}
				if(st.size() != 3 || !st.get(0).matches("^\\d{3}$") || !st.get(1).matches("^[0-9a-zA-Z]{8}$") || !st.get(2).matches("^\\d{1,10}$")){
					System.out.println(fl.getName() + "のフォーマットが不正です");
					return false;
				}
				if(!branchCodeAndNameMap.containsKey(st.get(0))){
					System.out.println(fl.getName() + "の支店コードが不正です");
					return false;
				}
				if(!commodityCodeAndNameMap.containsKey(st.get(1))){
					System.out.println(fl.getName() + "の商品コードが不正です");
					return false;
				}

				branchTatalSaleMap.put(st.get(0), branchTatalSaleMap.get(st.get(0)) + Long.parseLong(st.get(2)));
				if(!branchTatalSaleMap.get(st.get(0)).toString().matches("^\\d{1,10}$")){
					System.out.println("合計金額が10桁を超えました");
					return false;
				}
				commodityTatalSaleMap.put(st.get(1), commodityTatalSaleMap.get(st.get(1)) + Long.parseLong(st.get(2)));
				if(!commodityTatalSaleMap.get(st.get(1)).toString().matches("^\\d{1,10}$")){
					System.out.println("合計金額が10桁を超えました");
					return false;
				}
			}catch(IOException e){
				System.out.println("予期せぬエラーが発生しました");
				return false;
			}finally{
				if(br != null){
					br.close();
				}
			}
		}
		return true;
	}
	/*
	 * 売上ファイルの書出し
	 * readfile(String st, HashMap<String,String> fl, HashMap<String,Long> flsum)
	 *@param filename
	 *@param 定義ファイル
	 *@param 売上合計ファイル
	 *@error 予期せぬエラー
	 */
	public static boolean writeTotalSaleFile(File outfile, HashMap<String, String> codeAndName, HashMap<String, Long> TatalSaleMap) throws IOException{
		//sort
		List<Map.Entry<String, Long>> entries1 = new ArrayList<Map.Entry<String, Long>>(TatalSaleMap.entrySet());
		Collections.sort(entries1, new Comparator<Map.Entry<String, Long>>() {
			public int compare(Entry<String, Long> entry1, Entry<String, Long> entry2) {
				return ((Long)entry2.getValue()).compareTo((Long)entry1.getValue());
				}
			});
		//write file
		BufferedWriter bw =null;
		try{
			FileWriter fw = new FileWriter(outfile);
			bw = new BufferedWriter(fw);
			for(Entry<String, Long> sortedToalSale : entries1){
				bw.write(sortedToalSale.getKey() + "," + codeAndName.get(sortedToalSale.getKey()) + "," + sortedToalSale.getValue().toString());
				bw.newLine();
			}
			return true;
		}catch(IOException e){
			System.out.println("予期せぬエラーが発生しました");
			return false;
		}finally{
			if(bw != null){
				bw.close();
			}
		}
	}

}
